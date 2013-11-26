package com.readytalk.swt.widgets.buttons;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.SWT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * New, improved, full of youth and vigor!  Behold SquareButton 2.0!
 *
 * Some things have changed (duh), so here's the skinny.
 *
 * The SquareButton class is a simple SWT widget that can be used in place of
 * a native SWT button. Unlike the native button, this should look exactly the
 * same on all platforms, and about a million times better than most native buttons.
 *
 * Because I'm lazy and hate boilerplate, 2.0 now uses a Builder to speed up construction
 * and make things less ugly. Here's an example:
 * <code>
 *              SquareButton.SquareButtonBuilder builder = new SquareButton.SquareButtonBuilder();<br/>
 *              SquareButton button = builder.setParent(composite).setImage(image).setText("Bon jour!).build();<br/>
 *              button.addMouseListener(new MouseAdapter() {
 *                @Overrides
 *                public void mouseUp(MouseEvent mouseEvent) {
 *                  magic();
 *                }
 *              }
 *
 * </code>
 * Julian (the OG author) set default colors to a pretty basic pallet that he was modelling after the stock GMail interface.
 * It's fine, but you'll probably want to use your own, hence the SquareButtonColorGroups.  You can specify gradients for all
 * possible states of the button.
 *
 * You can also specify a background image to be used, either cropped, centered, stretched, tiled, or set
 * such that the button is exactly the size of the background image.
 *
 * Version 2.0 adds the ability to place the image above the text using the ImagePadding enum, more centering options,
 * as well as some cleaner internal logic (if you find bugs).
 *
 * This code may be used under the terms of the Apache license, version 2.0.
 * Version 1.0 Copyright 2009-2010 Strategic Network Applications, Inc. (http://www.snapps.com).
 * Version 2.0 Copyright 2013 ReadyTalk (http://www.readytalk.com) NOTE: do we need this?
 *
 * @author Julian Robichaux
 * @author Matt Weaver
 * @version 2.0
 *
 */

public class SegmentedSquareButton extends SquareButton {
  protected static final Logger log = Logger.getLogger(SquareButton.class.getName());

  protected ArrayList<SegmentedSquareButtonItem> items = new ArrayList<SegmentedSquareButtonItem>();

  protected Listener keyListener;
  protected Font defaultFont;
  protected Color defaultFontColor, defaultHoverFontColor, defaultClickedFontColor, defaultInactiveFontColor, defaultSelectedFontColor;
  protected Color defaultBorderColor, defaultHoverBorderColor, defaultClickedBorderColor, defaultInactiveBorderColor, defaultSelectedBorderColor;
  protected Color defaultCurrentColor, defaultCurrentColor2, defaultCurrentFontColor, defaultCurrentBorderColor;
  protected Color defaultBackgroundColor, defaultBackgroundColor2;
  protected Color defaultClickedColor, defaultClickedColor2;
  protected Color defaultHoverColor, defaultHoverColor2;
  protected Color defaultInactiveColor, defaultInactiveColor2;
  protected Color defaultSelectedColor, defaultSelectedColor2;
  protected int innerMarginWidth = 8;
  protected int innerMarginHeight = 4;
  protected int borderWidth = 1;
  protected int imagePadding = 5; //5;
  protected boolean roundedCorners = true;
  protected int cornerRadius = 5;
  protected boolean selectionBorder = false;

  protected int lastWidth, lastHeight;

  public static final int BG_IMAGE_STRETCH = 1;
  public static final int BG_IMAGE_TILE = 2;
  public static final int BG_IMAGE_CENTER = 3;
  public static final int BG_IMAGE_FIT = 4;
  protected int backgroundImageStyle = 0;

  public enum ImagePosition {
    ABOVE_TEXT,
    BELOW_TEXT,
    RIGHT_OF_TEXT,
    LEFT_OF_TEXT
  }

  protected HashMap<String, Color> colorRegistry = new HashMap<String, Color>();

  protected SegmentedSquareButton(Composite parent, int style, SegmentedSquareButtonItem ... items) {
    super(parent, style | SWT.NO_BACKGROUND);
    this.setBackgroundMode(SWT.INHERIT_DEFAULT);

    setDefaultColors();
    addListeners();

    for(SegmentedSquareButtonItem item : items) {
      this.items.add(item);
    }
  }


  protected void widgetDisposed(DisposeEvent e) {
    // TODO clean up here (listeners?)
  }


  protected void addListeners() {
    addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        SegmentedSquareButton.this.widgetDisposed(e);
      }
    });

    addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        SegmentedSquareButton.this.paintControl(e);
      }
    });

    // MOUSE EVENTS will mostly apply to segments
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent mouseEvent) {
        if (mouseEvent.button == 1) {
          SegmentedSquareButton.this.setClickedColor();
        }
        super.mouseDown(mouseEvent);
      }

      @Override
      public void mouseUp(MouseEvent mouseEvent) {
        if (mouseEvent.button == 1) {
          SegmentedSquareButtonItem item = findItem(mouseEvent.x, mouseEvent.y);
          if ((mouseEvent.count == 1) && getEnabled() && (item != null)) {
            item.mouseUpEventRecieved();
          }
        }
        super.mouseUp(mouseEvent);
      }
    });

    addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent mouseEvent) {
        SegmentedSquareButtonItem item = findItem(mouseEvent.x, mouseEvent.y);
        if(item != null) {
          item.mouseEnterEventRecieved();
        }
        super.mouseEnter(mouseEvent);
      }

      @Override
      public void mouseExit(MouseEvent mouseEvent) {
        SegmentedSquareButtonItem item = findItem(mouseEvent.x, mouseEvent.y);
        if(item != null) {
          item.mouseExitEventRecieved();
        }

        super.mouseExit(mouseEvent);
      }

      @Override
      public void mouseHover(MouseEvent mouseEvent) {
        SegmentedSquareButtonItem item = findItem(mouseEvent.x, mouseEvent.y);
        if(item != null) {
          item.mouseHoverEventRecieved();
        }
        super.mouseHover(mouseEvent);
      }
    });

    // TAB TRAVERSAL (a KeyDown listener is also required)
    addListener(SWT.Traverse, new Listener() {
      public void handleEvent(Event e) {
        switch (e.detail) {
          case SWT.TRAVERSE_ESCAPE:
          case SWT.TRAVERSE_RETURN:
          case SWT.TRAVERSE_TAB_NEXT:
          case SWT.TRAVERSE_TAB_PREVIOUS:
          case SWT.TRAVERSE_PAGE_NEXT:
          case SWT.TRAVERSE_PAGE_PREVIOUS:
            e.doit = true;
            break;
          default:
            break;
        }
      }
    });
    addListener(SWT.FocusIn, new Listener () {
      public void handleEvent (Event e) {
        SegmentedSquareButtonItem item = findItem(e.x, e.y);
        if(item != null) {
          item.isFocused = true;
          item.setSelectedColor();
          redraw();
        }
      }
    });
    addListener(SWT.FocusOut, new Listener () {
      public void handleEvent (Event e) {
        SegmentedSquareButtonItem item = findItem(e.x, e.y);
        if(item != null) {
          item.isFocused = false;
          item.setNormalColor();
          redraw();
        }
      }
    });

    addListener(SWT.KeyUp, new Listener () {
      public void handleEvent (Event e) {
        SegmentedSquareButtonItem item = findItem(e.x, e.y);
        if(item != null) {
          item.isFocused = true;
          item.setSelectedColor();
          redraw();
        }
      }
    });
    keyListener = new Listener() {
      public void handleEvent(Event e) {
        // required for tab traversal to work
        SegmentedSquareButtonItem item = findItem(e.x, e.y);
        if(item != null) {

          switch (e.character) {
            case ' ':
            case '\r':
            case '\n':
              item.setClickedColor();
              redraw();
              doButtonClicked();
              break;
            default:
              break;
          }
        }
      }
    };
    setTraversable(true);
  }


  /**
   * SelectionListeners are notified when the button is clicked
   *
   * @param listener
   */
  public void addSelectionListener(SelectionListener listener) {
    addListener(SWT.Selection, new TypedListener(listener));
  }
  public void removeSelectionListener(SelectionListener listener) {
    removeListener(SWT.Selection, listener);
  }


  protected void setTraversable(boolean canTraverse) {
    // TODO is there a better way to do this?
    try {
      if (canTraverse)
        this.addListener (SWT.KeyDown, keyListener);
      else if (!canTraverse)
        this.removeListener(SWT.KeyDown, keyListener);
    } catch (Exception e) {}
  }


  protected void doButtonClicked() {
    Event e = new Event();
    e.item = this;
    e.widget = this;
    e.type = SWT.Selection;
    notifyListeners(SWT.Selection, e);
  }


  protected void setDefaultColors() {
    defaultFontColor = getSavedColor(0, 0, 0);
    defaultHoverFontColor = getSavedColor(0, 0, 0);
    defaultClickedFontColor = getSavedColor(255, 255, 255);
    defaultInactiveFontColor = getSavedColor(187, 187, 187);
    defaultSelectedFontColor = getSavedColor(160, 107, 38);
    defaultBorderColor = getSavedColor(187, 187, 187);
    defaultHoverBorderColor = getSavedColor(147, 147, 147);
    defaultClickedBorderColor = getSavedColor(147, 147, 147);
    defaultInactiveBorderColor = getSavedColor(200, 200, 200);
    defaultSelectedBorderColor = getSavedColor(160, 107, 38);
    defaultBackgroundColor = getSavedColor(248, 248, 248);
    defaultBackgroundColor2 = getSavedColor(228, 228, 228);
    defaultClickedColor = getSavedColor(120, 120, 120);
    defaultClickedColor2 = getSavedColor(150, 150, 150);
    defaultHoverColor = getSavedColor(248, 248, 248);
    defaultHoverColor2 = getSavedColor(228, 228, 228);
    defaultInactiveColor = getSavedColor(248, 248, 248);
    defaultInactiveColor2 = getSavedColor(228, 228, 228);
    defaultSelectedColor = getSavedColor(238, 238, 238);
    defaultSelectedColor2 = getSavedColor(218, 218, 218);
  }


  protected Color getSavedColor(int r, int g, int b) {
    String colorString = "SB_DEFAULT:" + r + "-" + g + "-" + b;
    if (!colorRegistry.containsKey(colorString)) {
      colorRegistry.put(colorString, new Color(null,r, g, b));
    }
    return colorRegistry.get(colorString);
  }


  protected void paintSegment(SegmentedSquareButtonItem item) {

  }


  protected void paintControl(PaintEvent paintEvent) {
    if (defaultCurrentColor == null) {
      defaultCurrentColor = defaultBackgroundColor;
      defaultCurrentColor2 = defaultBackgroundColor2;
      defaultCurrentBorderColor = defaultBorderColor;
      defaultCurrentFontColor = defaultFontColor;
    }

    Point buttonSize = computeSize();

    // with certain layouts, the width is sometimes 1 pixel too wide
    if (buttonSize.x > getClientArea().width) {
      buttonSize.x = getClientArea().width;
    }

    Rectangle buttonRectangle = new Rectangle(0, 0, buttonSize.x, buttonSize.y);

    GC gc = paintEvent.gc;

    //try and enable nice looking fonts on windows
    try {
      gc.setAdvanced(true);
      gc.setAntialias(SWT.ON);
      gc.setTextAntialias(SWT.ON);
      gc.setInterpolation(SWT.HIGH);
    } catch (SWTException exception) {
      log.warning("Couldn't get nice fonts: " + exception.getMessage());
    }

    // add transparency by making the canvas background the same as
    // the parent background (only needed for rounded corners)
    if (roundedCorners) {
      gc.setBackground(getParent().getBackground());
      gc.fillRectangle(buttonRectangle);
    }

    // draw the background color of the inside of the button. There's no such
    // thing as a rounded gradient rectangle in SWT, so we need to draw a filled
    // rectangle that's just the right size to fit inside a rounded rectangle
    // without spilling out at the corners
    gc.setForeground(this.defaultCurrentColor);
    gc.setBackground(this.defaultCurrentColor2);

    Rectangle fillRectangle = (roundedCorners) ? new Rectangle(buttonRectangle.x + 1, buttonRectangle.y + 1, buttonRectangle.width - 2, buttonRectangle.height - 3) :
      new Rectangle(buttonRectangle.x + 1, buttonRectangle.y + 1, buttonRectangle.width - 1, buttonRectangle.height - 1);

    gc.fillGradientRectangle(fillRectangle.x, fillRectangle.y, fillRectangle.width, fillRectangle.height, true);

    // if there's a background image, draw it on top of the interior color
    // so any image transparency allows the button colors to come through
    drawBackgroundImage(gc, fillRectangle);


    // draw the border of the button. If a zero-pixel border was specified,
    // draw a 1-pixel border the same color as the canvas background instead
    // so the rounded corners look right
    gc.setLineStyle(SWT.LINE_SOLID);

    int arcHeight = Math.max(cornerRadius, buttonSize.y / 10);
    int arcWidth = arcHeight;

    int bw = borderWidth;
    if (borderWidth > 0) {
      gc.setLineWidth(borderWidth);
      gc.setForeground(this.defaultCurrentBorderColor);
    } else {
      bw = 1;
      gc.setLineWidth(1);
      gc.setForeground(getParent().getBackground());
      arcWidth = arcHeight += 1;
    }
    if (roundedCorners) {
      gc.drawRoundRectangle(buttonRectangle.x + (bw - 1), buttonRectangle.y + (bw - 1), buttonRectangle.width - bw, buttonRectangle.height - 2, arcWidth, arcHeight);
    } else {
      gc.drawRectangle(buttonRectangle.x, buttonRectangle.y, buttonRectangle.width - bw, buttonRectangle.height - 1);
    }

    // dotted line selection border around the text, if any
    if (this.isFocused && this.selectionBorder) {
      gc.setForeground(defaultCurrentFontColor);
      gc.setLineStyle(SWT.LINE_DASH);
      gc.setLineWidth(1);
      gc.drawRectangle(buttonRectangle.x + (bw + 1), buttonRectangle.y + (bw + 1), buttonRectangle.width - (bw + 5), buttonRectangle.height - (bw + 5));
    }

    ContentOriginSet contentOriginSet = getContentsOrigin(buttonRectangle);
    Point textOrigin = contentOriginSet.getTextOrigin();
    Point imageOrigin = contentOriginSet.getImageOrigin();

    /*
     * Order of drawing matters... if the image is above or to the left,
     * it needs to be drawn first.
     */
    switch(imagePosition) {
      case LEFT_OF_TEXT:
      case ABOVE_TEXT:
        if(imageOrigin != null) {
          drawImage(gc, imageOrigin.x, imageOrigin.y);
        }
        if(textOrigin != null) {
          drawText(gc, textOrigin.x, textOrigin.y);
        }
        break;
      case RIGHT_OF_TEXT:
        if(textOrigin != null) {
          drawText(gc, textOrigin.x, textOrigin.y);
        }
        if(imageOrigin != null) {
          drawImage(gc, imageOrigin.x, imageOrigin.y);
        }
        break;
    }
  }

  /**
   * for LEFT_OF_TEXT and RIGHT_OF_TEXT
   *    widthOfContents = image.width + image.padding
   *                             + text.width
   * for ABOVE_TEXT
   *  widthOfContents = Math.max(image.width, text.width)
   * @return
   */
  protected int getWidthOfContents(SegmentedSquareButtonItem item) {
    int widthOfContents = 0;

    Point textSize = computeTextSize(item);
    Point imageSize = computeImageSize(item);

    int textWidth = (textSize != null) ? textSize.x : 0;
    int imageWidth = (imageSize != null) ? imageSize.x : 0;

    switch(imagePosition) {
      case LEFT_OF_TEXT:
      case RIGHT_OF_TEXT:
        widthOfContents = imageWidth + imagePadding + textWidth;
        break;
      case ABOVE_TEXT:
        widthOfContents = Math.max(textWidth, imageWidth);
        break;
    }
    return widthOfContents;
  }

  /**
   * for LEFT_OF_TEXT and RIGHT_OF_TEXT
   *  heightOfContents = Math.max(textHeight, imageHeight);
   *
   * for ABOVE_TEXT
   *  heightOfContents = imageHeight + imagePadding + textHeight
   * @return
   */
  protected int getHeightOfContents(SegmentedSquareButtonItem item) {
    int heightOfContents = 0;

    Point textSize = computeTextSize();
    Point imageSize = computeImageSize();

    int textHeight = (textSize != null) ? textSize.y : 0;
    int imageHeight = (imageSize != null) ? imageSize.y : 0;

    switch(imagePosition) {
      case LEFT_OF_TEXT:
      case RIGHT_OF_TEXT:
        heightOfContents = Math.max(textHeight, imageHeight);
        break;
      case ABOVE_TEXT:
        heightOfContents = imageHeight + imagePadding + textHeight;
        break;
    }
    return heightOfContents;
  }

  /**
   * Trying to consolidate this for sanity's sake.
   *
   * @return
   */
  protected ContentOriginSet getContentsOrigin(Rectangle rectangle) {
    int imageX, imageY, textX, textY;
    imageX = imageY = textX = textY = 0;
    Point textSize = computeTextSize();
    if(textSize == null) {
      textSize = new Point(0,0);
    }
    Point imageSize = computeImageSize();
    if(imageSize == null) {
      imageSize = new Point(0,0);
    }

    boolean imageWider = imageSize.x > textSize.x;
    boolean imageTaller = imageSize.y > textSize.y;

    int widthOfContents = getWidthOfContents();
    int heightOfContents = getHeightOfContents();
      /*
       * For clarity's sake remember a few things... we depend on the button
       * rectangle being sized at this point, which will take into account the
       * inner margin values (which we point out here for clarity's sake).
       *
       * The functions that calculate the width and height of contents change
       * how that is done based on the three possible configurations of a button.
       *
       * Typically, text will be wider than images, but shorter in height.  However,
       * this won't always be true which is why the getWidthOfContents and getHeightOfContents
       * will use a Math.max call to determine which one is taller in the cases where they may be different.
       */

    switch (imagePosition) {
      case LEFT_OF_TEXT:
        /*
         * LEFT_OF_TEXT
         *           1. innerMarginWidth
         *           2. innerMarginHeight
         *           3. image.width
         *           4. image.height
         *           5. image.padding
         *           6. text.width
         *           7. text.height
         *  ________________________________________
         *  |           ^                           |
         *  |           2                           |
         *  |           |                           |
         *  |      ########### ^                    |
         *  |      #         # |                    |
         *  |<--1-># ^     ^ # |   <----6-----><-1->|
         *  |      #         # 4   CEREAL BAWX  ^   |
         *  |      #   ~~~   #<-5->BEFORE I GET 7   |
         *  |      #         # |   ANGRY!!!     |   |
         *  |      ########### |                    |
         *  |      <-3--^---->                      |
         *  |           2                           |
         *  |           |                           |
         *  |_______________________________________|
         */

        imageX = (horizontallyCenterContents) ? rectangle.width/2 - widthOfContents/2 : innerMarginWidth;
        textX = imageX + imageSize.x + imagePadding;

        // anchor height based on the image, if the image is the taller of the two... BUT that might not always be the case
        if(imageTaller) {
          imageY = (verticallyCenterContents) ? rectangle.height/2 - imageSize.y/2 : innerMarginHeight;
          textY = imageY + imageSize.y/2 - textSize.y/2;
        } else {
          textY = (verticallyCenterContents) ? rectangle.height/2 - textSize.y/2 : innerMarginHeight;
          imageY = textY + textSize.y/2 - imageSize.y/2;
        }

        break;
      case RIGHT_OF_TEXT:
        /*  RIGHT_OF_TEXT
         *           1. innerMarginWidth
         *           2. innerMarginHeight
         *           3. image.width
         *           4. image.height
         *           5. image.padding
         *           6. text.width
         *           7. text.height
         *  ________________________________________
         *  |                            ^          |
         *  |                            2          |
         *  |                            |          |
         *  |                       ########### ^   |
         *  |                       #         # |   |
         *  |<--1-><----6-----><-5-># ^     ^ #<-1->|
         *  |      CEREAL BAWX  ^   #         # 4   |
         *  |      BEFORE I GET 7   #   ~~~   # |   |
         *  |      ANGRY!!!     |   #         # |   |
         *  |                       ########### |   |
         *  |                       <-3--^---->     |
         *  |                            2          |
         *  |                            |          |
         *  |_______________________________________|
         */
        textX = (horizontallyCenterContents) ? rectangle.width/2 - widthOfContents/2 : innerMarginWidth;
        imageX = textX + textSize.x + imagePadding;

        // anchor height based on the image, if the image is the taller of the two... BUT that might not always be the case
        if(imageTaller) {
          imageY = (verticallyCenterContents) ? rectangle.height/2 - imageSize.y/2 : innerMarginHeight;
          textY = imageY + imageSize.y/2 - textSize.y/2;
        } else {
          textY = (verticallyCenterContents) ? rectangle.height/2 - textSize.y/2 : innerMarginHeight;
          imageY = textY + textSize.y/2 - imageSize.y/2;
        }
        break;
      case ABOVE_TEXT:
        /*  ABOVE_TEXT
         *           1. innerMarginWidth (Whichever is wider, the image or the text)
         *           2. innerMarginHeight
         *           3. image.width
         *           4. image.height
         *           5. image.padding
         *           6. text.width
         *           7. text.height
         *  ___________________________
         *  |            ^            |
         *  |            2            |
         *  |            |            |
         *  |       ########### ^     |
         *  |       #         # |     |
         *  |       # ^     ^ # |     |
         *  |       #         # 4     |
         *  |       #   ~~~   # |     |
         *  |       #         # |     |
         *  |       ########### |     |
         *  |<--1--><-3--^---->       |
         *  |            |            |
         *  |   O        5            |
         *  |   R        |            |
         *  |            |            |
         *  |<--1-><--6--|----><--1-->|
         *  |      CEREAL BAWX  ^     |
         *  |      BEFORE I GET 7     |
         *  |      ANGRY!!!     |     |
         *  |                         |
         *  |                         |
         *  |                         |
         *  |                         |
         *  |_________________________|
         */
        imageY = (verticallyCenterContents) ? rectangle.height/2 - heightOfContents/2 : innerMarginHeight;
        textY = imageY + imageSize.y + imagePadding;

        // anchor width based on the image only if the image is wider, probably not likely but possible
        if(imageWider) {
          imageX = (horizontallyCenterContents) ? rectangle.width/2 - imageSize.x/2 : innerMarginWidth;
          textX = imageX + imageSize.x/2 - textSize.x/2;
        } else {
          textX = (horizontallyCenterContents) ? rectangle.width/2 - textSize.x/2 : innerMarginWidth;
          imageX = textX + textSize.x/2 - imageSize.x/2;
        }
        break;
    }
    Point imageOrigin = (image != null) ? new Point(imageX, imageY) : null;
    Point textOrigin = (text != null) ? new Point(textX, textY) : null;
    return new ContentOriginSet(imageOrigin, textOrigin);
  }

  protected void drawText(GC gc, int x, int y) {
    gc.setFont(defaultFont);
    gc.setForeground(defaultCurrentFontColor);
    gc.drawText(text, x, y, SWT.DRAW_TRANSPARENT | SWT.DRAW_DELIMITER);
  }


  protected int drawImage(GC gc, int x, int y) {
    if (image == null)
      return x;
    gc.drawImage(image, x, y);
    return x + image.getBounds().width + imagePadding;
  }


  protected void drawBackgroundImage(GC gc, Rectangle rect) {
    if (backgroundImage == null)
      return;

    Rectangle imgBounds = backgroundImage.getBounds();

    if (backgroundImageStyle == BG_IMAGE_STRETCH) {
      gc.drawImage(backgroundImage, 0, 0, imgBounds.width, imgBounds.height, rect.x, rect.y, rect.width, rect.height);

    } else if (backgroundImageStyle == BG_IMAGE_CENTER) {
      int x = (imgBounds.width - rect.width) / 2;
      int y = (imgBounds.height - rect.height) / 2;
      Rectangle centerRect = new Rectangle(rect.x, rect.y, rect.width, rect.height);
      if (x < 0) {
        centerRect.x -= x;
        x = 0;
      }
      if (y < 0) {
        centerRect.y -= y;
        y = 0;
      }
      drawClippedImage(gc, backgroundImage, x, y, centerRect);

    } else if (backgroundImageStyle == BG_IMAGE_TILE) {
      for (int y = 0; y < rect.height; y += imgBounds.height) {
        Rectangle tileRect = new Rectangle(rect.x, y + rect.y, rect.width, rect.height-y);

        for (int x = 0; x < rect.width; x += imgBounds.width) {
          tileRect.x += drawClippedImage(gc, backgroundImage, 0, 0, tileRect);
          tileRect.width -= x;
        }
      }

    } else {
      // default is BG_IMAGE_CROP
      drawClippedImage(gc, backgroundImage, 0, 0, rect);
    }
  }

  protected int drawClippedImage(GC gc, Image image, int x, int y, Rectangle rect) {
    if (image != null) {
      Rectangle imageBounds = image.getBounds();
      int width = Math.min(imageBounds.width - x, rect.width);
      int height = Math.min(imageBounds.height - y, rect.height);
      gc.drawImage(image, x, y, width, height, rect.x, rect.y, width, height);
      return width;
    }
    return 0;
  }

  public Point computeSegmentSize(SegmentedSquareButtonItem item) {
    return computeSize(item, SWT.DEFAULT, SWT.DEFAULT, false);
  }

  public Point computeSegmentSize(SegmentedSquareButtonItem item, int widthHint, int heightHint, boolean changed) {
    Point size = null;
    if ((widthHint == SWT.DEFAULT) && (heightHint == SWT.DEFAULT) && !changed &&
      (lastWidth > 0) && (lastHeight > 0)) {
      size = new Point(lastWidth, lastHeight);
    } else {
      int width = (widthHint != SWT.DEFAULT) ? widthHint : getWidthOfContents() + (innerMarginWidth * 2);
      int height = (heightHint != SWT.DEFAULT) ? heightHint : getHeightOfContents() + (innerMarginHeight * 2);

      /*
       * In this case, it's possible that the background image will be small and
       * your text will get junky... careful with this one, kid.
       */
      if ((backgroundImage != null) && (backgroundImageStyle == BG_IMAGE_FIT)) {
        width = backgroundImage.getBounds().width;
        height = backgroundImage.getBounds().height;
      }

      lastWidth = width + 2;
      lastHeight = height + 2;
      size = new Point(lastWidth, lastHeight);
    }
    return size;
  }

  public Point computeTextSize(SegmentedSquareButtonItem item) {
    Point size = null;
    if (item.getText() != null) {
      GC gc = new GC(this);
      gc.setFont(defaultFont);
      size = gc.textExtent(text, SWT.DRAW_DELIMITER);
      gc.dispose();
    }
    return size;
  }

  public Point computeImageSize(SegmentedSquareButtonItem item) {
    Point size = null;
    if(item.getImage() != null) {
      Rectangle imageBounds = image.getBounds();
      size = new Point(imageBounds.width, imageBounds.height);
    }
    return size;
  }

  /**
   * This is an image that will be displayed to the side of the
   * text inside the button (if any). By default the image will be
   * to the left of the text; however, setImageStyle can be used to
   * specify that it's either to the right or left. If there is no
   * text, the image will be centered inside the button.
   *
   * @param image
   */
  public void setImage(Image image) {
    this.image = image;
    redraw();
  }
  public Image getImage() {
    return image;
  }

  public int getImagePadding() {
    return imagePadding;
  }

  public void setImagePadding(int imagePadding) {
    this.imagePadding = imagePadding;
  }


  /**
   * This is an image that will be used as a background image for the
   * button, drawn in the manner specified by the backgroundImageStyle
   * setting. The order in which the button is drawn is: background
   * color, then background image, then button image and text. So if the
   * background image has transparency, the background color will show
   * through the transparency.
   */
  public void setBackgroundImage(Image backgroundImage) {
    this.backgroundImage = backgroundImage;
    redraw();
  }
  public Image getBackgroundImage() {
    return backgroundImage;
  }

  /**
   * Set the style with which the background image is drawn (default is
   * BG_IMAGE_CROP). The different styles are:
   * <p><ul>
   * <li>BG_IMAGE_CROP: the image is drawn once, with the top left corner
   * of the image at the top left corner of the button. Any part of the image
   * that is too wide or too tall to fit inside the button area is clipped
   * (cropped) off.</li>
   * <li>BG_IMAGE_STRETCH: the image is stretched (or squashed) to exactly
   * fit the button area.</li>
   * <li>BG_IMAGE_TILE: the image is tiled vertically and horizontally to
   * cover the entire button area.</li>
   * <li>BG_IMAGE_CENTER: the center of the image is placed inside the center
   * of the button. Any part of the image that is too tall or too wide to fit
   * will be clipped.</li>
   * <li>BG_IMAGE_FIT: the button will be the exact size of the image. Note that
   * this can sometimes truncate the text inside the button.</li>
   * </ul>
   *
   * @param backgroundImageStyle
   */
  public void setBackgroundImageStyle(int backgroundImageStyle) {
    this.backgroundImageStyle = backgroundImageStyle;
  }
  public int getBackgroundImageStyle() {
    return backgroundImageStyle;
  }

  public String getText() {
    return text;
  }
  public void setText(String text) {
    this.text = text;
    redraw();
  }
  public Font getFont() {
    return defaultFont;
  }
  public void setFont(Font font) {
    if (font != null)
      this.defaultFont = font;
  }

  public boolean isHorizontallyCenterContents() {
    return horizontallyCenterContents;
  }

  public void setHorizontallyCenterContents(boolean horizontallyCenterContents) {
    this.horizontallyCenterContents = horizontallyCenterContents;
  }

  public boolean isVerticallyCenterContents() {
    return verticallyCenterContents;
  }

  public void setVerticallyCenterContents(boolean verticallyCenterContents) {
    this.verticallyCenterContents = verticallyCenterContents;
  }

  /**
   * Set whether or not this button is enabled (active) or
   * not (inactive). This setting can be changed dynamically
   * after the button has been drawn.
   * <p>
   * An inactive button does not change color
   * when it is hovered over or clicked, does not receive focus
   * or participate in the tab order of the widget container,
   * and does not notify listeners when clicked.
   */
  public void setEnabled(boolean enabled) {
    boolean oldSetting = getEnabled();
    super.setEnabled(enabled);
    if (oldSetting != enabled) {
      if (!enabled) {
        super.setEnabled(true);
        this.setInactiveColor();
        this.setTraversable(false);
        super.setEnabled(false);
      } else {
        this.setNormalColor();
        this.setTraversable(true);
      }
    }
  }

//  public boolean isEnabled() {
//    return enabled;
//  }

  /**
   * Set the inner margin between the left and right of the text
   * inside the button and the button borders, in pixels. Like the
   * left and right padding for the text. Default is 8 pixels.
   *
   * @param innerMarginWidth
   */
  public void setInnerMarginWidth(int innerMarginWidth) {
    if (innerMarginWidth >= 0)
      this.innerMarginWidth = innerMarginWidth;
  }
  public int getInnerMarginWidth() {
    return innerMarginWidth;
  }

  /**
   * Set the inner margin between the top and bottom of the text
   * inside the button and the button borders, in pixels. Like the
   * top and bottom padding for the text. Default is 4 pixels.
   *
   * @param innerMarginHeight
   */
  public void setInnerMarginHeight(int innerMarginHeight) {
    if (innerMarginHeight >= 0)
      this.innerMarginHeight = innerMarginHeight;
  }
  public int getInnerMarginHeight() {
    return innerMarginHeight;
  }

  /**
   * Set whether or not the button should have rounded corners
   * (default is true).
   *
   * @param roundedCorners
   */
  public void setRoundedCorners(boolean roundedCorners) {
    this.roundedCorners = roundedCorners;
  }
  public boolean hasRoundedCorners() {
    return roundedCorners;
  }

  /**
   * Set whether or not a dotted-line border should be drawn around
   * the text inside the button when the button has tab focus. Default is
   * false (no selection border). If a selection border is used, it will
   * be the same color as the defaultFont color. Note that you can also use
   * setSelectedColors() to change the look of the button when it has
   * focus.
   *
   * @param selectionBorder
   */
  public void setSelectionBorder(boolean selectionBorder) {
    this.selectionBorder = selectionBorder;
  }
  public boolean hasSelectionBorder() {
    return selectionBorder;
  }

  /**
   * Set the width of the button border, in pixels (default is 1).
   *
   * @param borderWidth
   */
  public void setBorderWidth(int borderWidth) {
    this.borderWidth = borderWidth;
  }
  public int getBorderWidth() {
    return borderWidth;
  }

  /**
   * The colors of the button in its "default" state (not clicked, selected, etc.)
   *
   * @param topBackgroundColor the gradient color at the top of the button
   * @param bottomBackgroundColor the gradient color at the bottom of the button (if you don't want a gradient, set this to topBackgroundColor)
   * @param borderColor the color of the border around the button (if you don't want a border, use getBackground())
   * @param fontColor the color of the defaultFont inside the button
   */
  public void setDefaultColors(Color topBackgroundColor, Color bottomBackgroundColor, Color borderColor, Color fontColor) {
    if (topBackgroundColor != null) { this.defaultBackgroundColor = topBackgroundColor; }
    if (bottomBackgroundColor != null) { this.defaultBackgroundColor2 = bottomBackgroundColor; }
    if (borderColor != null) { this.defaultBorderColor = borderColor; }
    if (fontColor != null) { this.defaultFontColor = fontColor; }
  }

  public void setDefaultColors(SquareButtonColorGroup squareButtonColorGroup) {
    setSelectedColors(squareButtonColorGroup.getTopBackgroundColor(),
      squareButtonColorGroup.getBottomBackgroundColor(),
      squareButtonColorGroup.getBorderColor(),
      squareButtonColorGroup.getFontColor());
  }

  /**
   * The colors of the button when the mouse is hovering over it
   *
   * @param topBackgroundColor the gradient color at the top of the button
   * @param bottomBackgroundColor the gradient color at the bottom of the button (if you don't want a gradient, set this to topBackgroundColor)
   * @param borderColor the color of the border around the button (if you don't want a border, use getBackground())
   * @param fontColor the color of the defaultFont inside the button
   */
  public void setDefaultHoverColors(Color topBackgroundColor, Color bottomBackgroundColor, Color borderColor, Color fontColor) {
    if (topBackgroundColor != null) { this.defaultHoverColor = topBackgroundColor; }
    if (bottomBackgroundColor != null) { this.defaultHoverColor2 = bottomBackgroundColor; }
    if (borderColor != null) { this.defaultHoverBorderColor = borderColor; }
    if (fontColor != null) { this.defaultHoverFontColor = fontColor; }
  }

  public void setDefaultHoverColors(SquareButtonColorGroup squareButtonColorGroup) {
    setHoverColors(squareButtonColorGroup.getTopBackgroundColor(),
      squareButtonColorGroup.getBottomBackgroundColor(),
      squareButtonColorGroup.getBorderColor(),
      squareButtonColorGroup.getFontColor());
  }

  /**
   * The colors of the button when it is being clicked (MouseDown)
   *
   * @param topBackgroundColor the gradient color at the top of the button
   * @param bottomBackgroundColor the gradient color at the bottom of the button (if you don't want a gradient, set this to topBackgroundColor)
   * @param borderColor the color of the border around the button (if you don't want a border, use getBackground())
   * @param fontColor the color of the defaultFont inside the button
   */
  public void setDefaultClickedColors(Color topBackgroundColor, Color bottomBackgroundColor, Color borderColor, Color fontColor) {
    if (topBackgroundColor != null) { this.defaultClickedColor = topBackgroundColor; }
    if (bottomBackgroundColor != null) { this.defaultClickedColor2 = bottomBackgroundColor; }
    if (borderColor != null) { this.defaultClickedBorderColor = borderColor; }
    if (fontColor != null) { this.defaultClickedFontColor = fontColor; }
  }

  public void setDefaultClickedColors(SquareButtonColorGroup squareButtonColorGroup) {
    setClickedColors(squareButtonColorGroup.getTopBackgroundColor(),
      squareButtonColorGroup.getBottomBackgroundColor(),
      squareButtonColorGroup.getBorderColor(),
      squareButtonColorGroup.getFontColor());
  }

  /**
   * The colors of the button when it has focus
   *
   * @param topBackgroundColor the gradient color at the top of the button
   * @param bottomBackgroundColor the gradient color at the bottom of the button (if you don't want a gradient, set this to topBackgroundColor)
   * @param borderColor the color of the border around the button (if you don't want a border, use getBackground())
   * @param fontColor the color of the defaultFont inside the button
   */
  public void setDefaultSelectedColors(Color topBackgroundColor, Color bottomBackgroundColor, Color borderColor, Color fontColor) {
    if (topBackgroundColor != null) { this.defaultSelectedColor = topBackgroundColor; }
    if (bottomBackgroundColor != null) { this.defaultSelectedColor2 = bottomBackgroundColor; }
    if (borderColor != null) { this.defaultSelectedBorderColor = borderColor; }
    if (fontColor != null) { this.defaultSelectedFontColor = fontColor; }
  }

  /**
   * Easier to go this route, son.
   *
   * @param squareButtonColorGroup
   */
  public void setDefaultSelectedColors(SquareButtonColorGroup squareButtonColorGroup) {
    setSelectedColors(squareButtonColorGroup.getTopBackgroundColor(),
      squareButtonColorGroup.getBottomBackgroundColor(),
      squareButtonColorGroup.getBorderColor(),
      squareButtonColorGroup.getFontColor());
  }

  /**
   * The colors of the button when it is in an inactive (not enabled) state
   *
   * @param topBackgroundColor the gradient color at the top of the button
   * @param bottomBackgroundColor the gradient color at the bottom of the button (if you don't want a gradient, set this to topBackgroundColor)
   * @param borderColor the color of the border around the button (if you don't want a border, use getBackground())
   * @param fontColor the color of the defaultFont inside the button
   */
  public void setDefaultInactiveColors(Color topBackgroundColor, Color bottomBackgroundColor, Color borderColor, Color fontColor) {
    if (topBackgroundColor != null) { this.defaultInactiveColor = topBackgroundColor; }
    if (bottomBackgroundColor != null) { this.defaultInactiveColor2 = bottomBackgroundColor; }
    if (borderColor != null) { this.defaultInactiveBorderColor = borderColor; }
    if (fontColor != null) { this.defaultInactiveFontColor = fontColor; }
  }

  /**
   * Much easier to use the inner class.
   *
   * @param squareButtonColorGroup
   */
  public void setDefaultInactiveColors(SquareButtonColorGroup squareButtonColorGroup) {
    setInactiveColors(squareButtonColorGroup.getTopBackgroundColor(),
      squareButtonColorGroup.getBottomBackgroundColor(),
      squareButtonColorGroup.getBorderColor(),
      squareButtonColorGroup.getFontColor());
  }

  public int getCornerRadius() {
    return cornerRadius;
  }

  public void setCornerRadius(int cornerRadius) {
    this.cornerRadius = cornerRadius;
  }

  protected void setAccessibilityName(final String name) {
    getAccessible().addAccessibleListener
      (new AccessibleAdapter() {
        public void getName(AccessibleEvent e) {
          e.result = name;
        }
      });
  }

  public void addItem(SegmentedSquareButtonItem item) {
    items.add(item);
  }

  public void addItem(int index, SegmentedSquareButtonItem item) {
    items.add(index, item);
  }

  protected SegmentedSquareButtonItem findItem(int x, int y) {
    SegmentedSquareButtonItem item = null;
    for(SegmentedSquareButtonItem i : items) {
      if(i.getRectangle().contains(x,y)) {
        item = i;
        break;
      }
    }
    return item;
  }

  private enum SegmentStyle {
    TEXT_ONLY,
    IMAGE_ONLY,
    IMAGE_AND_TEXT,
    BLANK
  }

  public abstract class SegmentedSquareButtonItem {
    private String text;
    protected Image image, backgroundImage;
    private String toolTip;
    private String accesibilityName;
    private Rectangle rectangle;
    private SegmentStyle segmentStyle;

    protected Font font;
    protected Color fontColor, hoverFontColor, clickedFontColor, inactiveFontColor, selectedFontColor;
    protected Color borderColor, hoverBorderColor, clickedBorderColor, inactiveBorderColor, selectedBorderColor;
    protected Color currentColor, currentColor2, currentFontColor, currentBorderColor;
    protected Color backgroundColor, backgroundColor2;
    protected Color clickedColor, clickedColor2;
    protected Color hoverColor, hoverColor2;
    protected Color inactiveColor, inactiveColor2;
    protected Color selectedColor, selectedColor2;

    boolean isFocused = false;

    protected ImagePosition imagePosition = ImagePosition.LEFT_OF_TEXT;

    protected boolean horizontallyCenterContents = false;
    protected boolean verticallyCenterContents = true;

    protected void checkSettings() {
      if(text != null && image != null) {
        segmentStyle = SegmentStyle.IMAGE_AND_TEXT;
      } else if(text != null && image == null) {
        segmentStyle = SegmentStyle.TEXT_ONLY;
      } else if(text == null && image != null) {
        segmentStyle = SegmentStyle.IMAGE_ONLY;
      } else {
        segmentStyle = SegmentStyle.BLANK;
      }
    }

    public String getText() {
      return text;
    }

    public SegmentedSquareButtonItem setText(String text) {
      this.text = text;
      checkSettings();
      return this;
    }

    public Image getImage() {
      return image;
    }

    public SegmentedSquareButtonItem setImage(Image image) {
      this.image = image;
      checkSettings();
      return this;
    }

    public String getToolTip() {
      return toolTip;
    }

    public SegmentedSquareButtonItem setToolTip(String toolTip) {
      this.toolTip = toolTip;
      return this;
    }

    public String getAccesibilityName() {
      return accesibilityName;
    }

    public SegmentedSquareButtonItem setAccesibilityName(String accesibilityName) {
      this.accesibilityName = accesibilityName;
      return this;
    }

    public ImagePosition getImagePosition() {
      return imagePosition;
    }

    public SegmentedSquareButtonItem setImagePosition(ImagePosition imagePosition) {
      this.imagePosition = imagePosition;
      return this;
    }

    protected SegmentStyle getSegmentStyle() {
      return segmentStyle;
    }

    protected Rectangle getRectangle() {
      return rectangle;
    }

    protected void setRectangle(Rectangle rectangle) {
      this.rectangle = rectangle;
    }

    public Image getBackgroundImage() {
      return backgroundImage;
    }

    public SegmentedSquareButtonItem setBackgroundImage(Image backgroundImage) {
      this.backgroundImage = backgroundImage;
      return this;
    }

    public SegmentedSquareButtonItem setSegmentStyle(SegmentStyle segmentStyle) {
      this.segmentStyle = segmentStyle;
      return this;
    }

    public Font getFont() {
      if(font == null) {
        font = defaultFont;
      }
      return font;
    }

    public SegmentedSquareButtonItem setFont(Font font) {
      this.font = font;
      return this;
    }

    public Color getFontColor() {
      if(fontColor == null) {
        fontColor = defaultFontColor;
      }
      return fontColor;
    }

    public SegmentedSquareButtonItem setFontColor(Color fontColor) {
      this.fontColor = fontColor;
      return this;
    }

    public Color getHoverFontColor() {
      if(hoverFontColor == null) {
        hoverFontColor = defaultHoverFontColor;
      }
      return hoverFontColor;
    }

    public SegmentedSquareButtonItem setHoverFontColor(Color hoverFontColor) {
      this.hoverFontColor = hoverFontColor;
      return this;
    }

    public Color getClickedFontColor() {
      if(clickedFontColor == null) {
        clickedFontColor = defaultClickedFontColor;
      }
      return clickedFontColor;
    }

    public SegmentedSquareButtonItem setClickedFontColor(Color clickedFontColor) {
      this.clickedFontColor = clickedFontColor;
      return this;
    }

    public Color getInactiveFontColor() {
      if(inactiveFontColor == null) {
        inactiveFontColor = defaultInactiveFontColor;
      }
      return inactiveFontColor;
    }

    public SegmentedSquareButtonItem setInactiveFontColor(Color inactiveFontColor) {
      this.inactiveFontColor = inactiveFontColor;
      return this;
    }

    public Color getSelectedFontColor() {
      if(selectedFontColor == null) {
        selectedFontColor = defaultSelectedFontColor;
      }
      return selectedFontColor;
    }

    public SegmentedSquareButtonItem setSelectedFontColor(Color selectedFontColor) {
      this.selectedFontColor = selectedFontColor;
      return this;
    }

    public Color getBorderColor() {
      if(borderColor == null) {
        borderColor = defaultBorderColor;
      }
      return borderColor;
    }

    public SegmentedSquareButtonItem setBorderColor(Color borderColor) {
      this.borderColor = borderColor;
      return this;
    }

    public Color getHoverBorderColor() {
      if(hoverBorderColor == null) {
        hoverBorderColor = defaultHoverBorderColor;
      }
      return hoverBorderColor;
    }

    public SegmentedSquareButtonItem setHoverBorderColor(Color hoverBorderColor) {
      this.hoverBorderColor = hoverBorderColor;
      return this;
    }

    public Color getClickedBorderColor() {
      if(clickedBorderColor == null) {
        clickedBorderColor = defaultClickedBorderColor;
      }
      return clickedBorderColor;
    }

    public SegmentedSquareButtonItem setClickedBorderColor(Color clickedBorderColor) {
      this.clickedBorderColor = clickedBorderColor;
      return this;
    }

    public Color getInactiveBorderColor() {
      if(inactiveBorderColor == null) {
        inactiveBorderColor = defaultInactiveBorderColor;
      }
      return inactiveBorderColor;
    }

    public SegmentedSquareButtonItem setInactiveBorderColor(Color inactiveBorderColor) {
      this.inactiveBorderColor = inactiveBorderColor;
      return this;
    }

    public Color getSelectedBorderColor() {
      if(selectedBorderColor == null) {
        selectedBorderColor = defaultSelectedBorderColor;
      }
      return selectedBorderColor;
    }

    public SegmentedSquareButtonItem setSelectedBorderColor(Color selectedBorderColor) {
      this.selectedBorderColor = selectedBorderColor;
      return this;
    }

    public Color getBackgroundColor() {
      return backgroundColor;
    }

    public SegmentedSquareButtonItem setBackgroundColor(Color backgroundColor) {
      this.backgroundColor = backgroundColor;
      return this;
    }

    public Color getBackgroundColor2() {
      return backgroundColor2;
    }

    public SegmentedSquareButtonItem setBackgroundColor2(Color backgroundColor2) {
      this.backgroundColor2 = backgroundColor2;
      return this;
    }

    public Color getClickedColor() {
      return clickedColor;
    }

    public SegmentedSquareButtonItem setClickedColor(Color clickedColor) {
      this.clickedColor = clickedColor;
      return this;
    }

    public Color getClickedColor2() {
      return clickedColor2;
    }

    public SegmentedSquareButtonItem setClickedColor2(Color clickedColor2) {
      this.clickedColor2 = clickedColor2;
      return this;
    }

    public Color getHoverColor() {
      return hoverColor;
    }

    public SegmentedSquareButtonItem setHoverColor(Color hoverColor) {
      this.hoverColor = hoverColor;
      return this;
    }

    public Color getHoverColor2() {
      return hoverColor2;
    }

    public SegmentedSquareButtonItem setHoverColor2(Color hoverColor2) {
      this.hoverColor2 = hoverColor2;
      return this;
    }

    public Color getInactiveColor() {
      return inactiveColor;
    }

    public SegmentedSquareButtonItem setInactiveColor(Color inactiveColor) {
      this.inactiveColor = inactiveColor;
      return this;
    }

    public Color getInactiveColor2() {
      return inactiveColor2;
    }

    public SegmentedSquareButtonItem setInactiveColor2(Color inactiveColor2) {
      this.inactiveColor2 = inactiveColor2;
      return this;
    }

    public Color getSelectedColor() {
      return selectedColor;
    }

    public SegmentedSquareButtonItem setSelectedColor(Color selectedColor) {
      this.selectedColor = selectedColor;
      return this;
    }

    public Color getSelectedColor2() {
      return selectedColor2;
    }

    public SegmentedSquareButtonItem setSelectedColor2(Color selectedColor2) {
      this.selectedColor2 = selectedColor2;
      return this;
    }

    Color getCurrentColor() {
      return currentColor;
    }

    void setCurrentColor(Color currentColor) {
      this.currentColor = currentColor;
    }

    Color getCurrentColor2() {
      return currentColor2;
    }

    void setCurrentColor2(Color currentColor2) {
      this.currentColor2 = currentColor2;
    }

    Color getCurrentFontColor() {
      return currentFontColor;
    }

    void setCurrentFontColor(Color currentFontColor) {
      this.currentFontColor = currentFontColor;
    }

    Color getCurrentBorderColor() {
      return currentBorderColor;
    }

    void setCurrentBorderColor(Color currentBorderColor) {
      this.currentBorderColor = currentBorderColor;
    }

    protected void setNormalColor() {
      setMouseEventColor(getBackgroundColor(), getBackgroundColor2(), getBorderColor(), getFontColor());
    }
    protected void setHoverColor() {
      setMouseEventColor(getHoverColor(), getHoverColor2(), getHoverBorderColor(), getHoverFontColor());
    }
    protected void setClickedColor() {
      setMouseEventColor(getClickedColor(), getClickedColor2(), getClickedBorderColor(), getClickedFontColor());
    }
    protected void setInactiveColor() {
      setMouseEventColor(getInactiveColor(), getInactiveColor2(), getInactiveBorderColor(), getInactiveFontColor());
    }
    protected void setSelectedColor() {
      setMouseEventColor(getSelectedColor(), getSelectedColor2(), getSelectedBorderColor(), getSelectedFontColor());
    }
    protected void setMouseEventColor(Color topBackgroundColor, Color bottomBackgroundColor, Color borderColor, Color fontColor) {
      if (!getEnabled())
        return;

      if (currentColor == null) {
        currentColor = defaultBackgroundColor;
        currentColor2 = defaultBackgroundColor2;
        currentBorderColor = borderColor;
        currentFontColor = fontColor;
      }

      boolean redrawFlag = false;
      if ((topBackgroundColor != null) && (!currentColor.equals(topBackgroundColor) ||
        !currentColor2.equals(bottomBackgroundColor))) {
        currentColor = currentColor2 = topBackgroundColor;
        if (bottomBackgroundColor != null) {
          currentColor2 = bottomBackgroundColor;
        }
        redrawFlag = true;
      }
      if ((borderColor != null) && (!currentBorderColor.equals(borderColor))) {
        defaultCurrentBorderColor = borderColor;
        redrawFlag = true;
      }
      if ((fontColor != null) && (!currentFontColor.equals(fontColor))) {
        currentFontColor = fontColor;
        redrawFlag = true;
      }
      if (redrawFlag) { redraw(); }
    }

    void mouseDoubleClickEventRecieved() {
      mouseDoubleClick();
    }

    void mouseDownEventRecieved() {
      mouseDown();
    }

    void mouseUpEventRecieved() {
      setHoverColor();
      mouseUp();
    }

    void mouseEnterEventRecieved() {
      setHoverColor();
      mouseEnter();
    }

    void mouseHoverEventRecieved() {
      setHoverColor();
      mouseHover();
    }

    void mouseExitEventRecieved() {
      if (isFocused) {
        setSelectedColor();
      } else {
        setNormalColor();
      }
      mouseExit();
    }

    public void mouseDoubleClick() { }
    public void mouseDown() { }
    public void mouseUp() { }
    public void mouseEnter() { }
    public void mouseHover() { }
    public void mouseExit() { }

  }

  public static class SquareButtonColorGroup {
    protected final Color topBackgroundColor;
    protected final Color bottomBackgroundColor;
    protected final Color borderColor;
    protected final Color fontColor;

    public SquareButtonColorGroup(Color topBackgroundColor, Color bottomBackgroundColor, Color borderColor, Color fontColor) {
      this.topBackgroundColor = topBackgroundColor;
      this.bottomBackgroundColor = bottomBackgroundColor;
      this.borderColor = borderColor;
      this.fontColor = fontColor;
    }

    public Color getTopBackgroundColor() {
      return topBackgroundColor;
    }

    public Color getBottomBackgroundColor() {
      return bottomBackgroundColor;
    }

    public Color getBorderColor() {
      return borderColor;
    }

    public Color getFontColor() {
      return fontColor;
    }
  }

  /**
   * For convenience
   */
  protected class ContentOriginSet {
    protected final Point imageOrigin;
    protected final Point textOrigin;

    protected ContentOriginSet(Point imageOrigin, Point textOrigin) {
      this.imageOrigin = imageOrigin;
      this.textOrigin = textOrigin;
    }

    protected Point getImageOrigin() {
      return imageOrigin;
    }

    protected Point getTextOrigin() {
      return textOrigin;
    }
  }

  /**
   * Not totally necessary, but it's lovely to chain a bunch of calls together for efficiency and brevity of code.  Looks bulky here,
   * but so much faster in implementation.
   */
  public static class SquareButtonBuilder {

    protected Composite parent;
    protected int style = SWT.NONE;
    protected String text;
    protected String toolTipText;
    protected Font font;
    protected Image image;
    protected ImagePosition imagePosition = ImagePosition.LEFT_OF_TEXT; // this is the default value of course
    protected boolean horizontallyCenterContents = false;
    protected boolean verticallyCenterContents = true;
    protected int imagePadding;
    protected int cornerRadius;
    protected SquareButtonColorGroup defaultColors;
    protected SquareButtonColorGroup selectedColors;
    protected SquareButtonColorGroup hoverColors;
    protected SquareButtonColorGroup clickedColors;
    protected SquareButtonColorGroup inactiveColors;
    protected String accessibilityName;

    public SquareButtonBuilder setParent(Composite parent) {
      this.parent = parent;
      return this;
    }

    public SquareButtonBuilder setStyle(int style) {
      this.style = style;
      return this;
    }

    public SquareButtonBuilder setText(String text) {
      this.text = text;
      return this;
    }

    public SquareButtonBuilder setToolTipText(String toolTipText) {
      this.toolTipText = toolTipText;
      return this;
    }

    public SquareButtonBuilder setFont(Font font) {
      this.font = font;
      return this;
    }

    public SquareButtonBuilder setImage(Image image) {
      this.image = image;
      return this;
    }

    public SquareButtonBuilder setImagePosition(ImagePosition imagePosition) {
      this.imagePosition = imagePosition;
      return this;
    }

    public SquareButtonBuilder setImagePadding(int imagePadding) {
      this.imagePadding = imagePadding;
      return this;
    }

    public SquareButtonBuilder setDefaultColors(SquareButtonColorGroup defaultColors) {
      this.defaultColors = defaultColors;
      return this;
    }

    public SquareButtonBuilder setSelectedColors(SquareButtonColorGroup selectedColors) {
      this.selectedColors = selectedColors;
      return this;
    }

    public SquareButtonBuilder setHoverColors(SquareButtonColorGroup hoverColors) {
      this.hoverColors = hoverColors;
      return this;
    }

    public SquareButtonBuilder setClickedColors(SquareButtonColorGroup clickedColors) {
      this.clickedColors = clickedColors;
      return this;
    }

    public SquareButtonBuilder setInactiveColors(SquareButtonColorGroup inactiveColors) {
      this.inactiveColors = inactiveColors;
      return this;
    }

    public SquareButtonBuilder setCornerRadius(int cornerRadius) {
      this.cornerRadius = cornerRadius;
      return this;
    }


    public SquareButtonBuilder setHorizontallyCenterContents(boolean horizontallyCenterContents) {
      this.horizontallyCenterContents = horizontallyCenterContents;
      return this;
    }

    public SquareButtonBuilder setVerticallyCenterContents(boolean verticallyCenterContents) {
      this.verticallyCenterContents = verticallyCenterContents;
      return this;
    }

    public SquareButtonBuilder setAccessibilityName(String accessibilityName) {
      this.accessibilityName = accessibilityName;
      return this;
    }

    public SquareButton build() throws IllegalArgumentException {
      SquareButton button = null;

      if(parent == null) {
        throw new IllegalArgumentException("You must set a parent.");
      }

      button = new SquareButton(parent, style);

      if(text != null) {
        button.setText(text);
      }

      if(toolTipText != null) {
        button.setToolTipText(toolTipText);
      }

      if(image != null) {
        button.setImage(image);
      }

      button.setImagePosition(imagePosition);
      button.setHorizontallyCenterContents(horizontallyCenterContents);
      button.setVerticallyCenterContents(verticallyCenterContents);

      if(imagePadding != 0) {
        button.setImagePadding(imagePadding);
      }

      if(cornerRadius != 0) {
        button.setRoundedCorners(true);
        button.setCornerRadius(cornerRadius);
      } else {
        button.setRoundedCorners(false); // not sure i want to do this: the default is ON, but I like the idea that the builder lets you do what you want
      }

      if(font != null) {
        button.setFont(font);
      }

      if(defaultColors != null) {
        button.setDefaultColors(defaultColors.getTopBackgroundColor(),
          defaultColors.getBottomBackgroundColor(),
          defaultColors.getBorderColor(),
          defaultColors.getFontColor());
      }

      if(selectedColors != null) {
        button.setSelectedColors(selectedColors.getTopBackgroundColor(),
          selectedColors.getBottomBackgroundColor(),
          selectedColors.getBorderColor(),
          selectedColors.getFontColor());
      }

      if(hoverColors != null) {
        button.setHoverColors(hoverColors.getTopBackgroundColor(),
          hoverColors.getBottomBackgroundColor(),
          hoverColors.getBorderColor(),
          hoverColors.getFontColor());
      }

      if(clickedColors != null) {
        button.setClickedColors(clickedColors.getTopBackgroundColor(),
          clickedColors.getBottomBackgroundColor(),
          clickedColors.getBorderColor(),
          clickedColors.getFontColor());
      }

      if(inactiveColors != null) {
        button.setInactiveColors(inactiveColors.getTopBackgroundColor(),
          inactiveColors.getBottomBackgroundColor(),
          inactiveColors.getBorderColor(),
          inactiveColors.getFontColor());
      }

      if(accessibilityName != null) {
        button.setAccessibilityName(accessibilityName);
      }

      return button;
    }
  }
}
