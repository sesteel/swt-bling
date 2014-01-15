package com.readytalk.swt.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * This is meant as a central repository for getting colors so that we don't allocate
 * duplicates everywhere, which greatly decreases the number of colors we allocate.
 * This currently doesn't do any reference counting or anything else, which we should
 * probably implement in the future.
 */
public class ColorFactory implements Runnable {
  private static final Logger LOG = Logger.getLogger(ColorFactory.class.getName());
  static Map<RGB, ColorReference> colorMap;
  static ReferenceQueue<Color> referenceQueue;
  static Thread thread;
  static long creationCount = 0;
  static long disposedCount = 0;

  static {
    colorMap = new HashMap<RGB, ColorReference>();
    referenceQueue = new ReferenceQueue<Color>();
    thread = new Thread(new ColorFactory());
    thread.setDaemon(true);
    thread.start();
  }

  /*
   * hidden constructor
   */
  private ColorFactory() {}

  static private class ColorReference extends PhantomReference<Color> {
    private RGB rgb;

    public ColorReference(final RGB rgb, final Color color, final ReferenceQueue<? super Color> referenceQueue) {
      super(color, referenceQueue);
      this.rgb = rgb;
    }

    @Override
    public Color get() {
      try {
        // PhantomReference's get always returns null; while WeakReference does not get
        // enqueued into the referenceQueue
        Field field = Reference.class.getDeclaredField("referent");
        field.setAccessible(true);
        return (Color)field.get(this);
      } catch (Exception e) {
        LOG.log(Level.SEVERE, e.getMessage(), e);
      }
      return null;
    }

    /*
     * Make sure no color reference escapes the scope of this method!
     */
    public void cleanup() {
      Color color = get();
      if (color != null && !color.isDisposed()) {
        disposedCount++;
        LOG.log(Level.INFO, "ColorFactory disposing " + color.toString() + " created: " + creationCount + " disposed:" + disposedCount);
        color.dispose();
        colorMap.remove(rgb);
      }
    }
  }

  public void run() {
    while (true) {
      try {
        Reference reference;
        while ((reference = referenceQueue.remove()) != null) {
          ColorReference colorReference = (ColorReference)reference;
          colorReference.cleanup();
        }
        Thread.sleep(1000);
      } catch (Exception e) {
        LOG.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }

  // this should use the current display
  public static Color getColor(int red, int green, int blue) {
    return getColor(Display.getCurrent(), red, green, blue);
  }

  /**
   * Get a pre-generated Color object based on the passed in parameters.
   * This Color object may be shared, so it SHOULD NOT be disposed except through this
   * class' disposeAll() method. The owner of the Color object is ColorFactory, not the
   * caller of this method.
   * @param device Device object needed to create the color
   * @param red
   * @param green
   * @param blue
   * @return A possibly shared Color object with the specified components
   */
  public static Color getColor(Device device, int red, int green, int blue) {
    RGB rgb = new RGB(red, green, blue);
    return getColor(device, rgb);
  }

  /**
   * Get a pre-generated Color object based on the passed in parameters.
   * This Color object may be shared, so it SHOULD NOT be disposed except through this
   * class' disposeAll() method. The owner of the Color object is ColorFactory, not the
   * caller of this method.
   * @param device Device object needed to create the color
   * @param rgb
   * @return A possibly shared Color object with the specified rgb values
   */
  public static Color getColor(Device device, RGB rgb) {
    Color color = null;
    ColorReference colorReference = colorMap.get(rgb);
    if (colorReference == null || (color = colorReference.get()) == null || color.isDisposed()) {
      creationCount++;
      LOG.log(Level.INFO, "ColorFactory creating " + rgb);
      color = new Color(device, rgb);
      colorReference = new ColorReference(rgb, color, referenceQueue);
      colorMap.put(rgb, colorReference);
    }
    return color;
  }

  /**
   * Disposes all the colors and clears the internal storage of colors. Does not do ref counting,
   * so use this with care.
   */
  public static void disposeAll() {
    for (Reference<Color> colorReference: colorMap.values()) {
      Color c = colorReference.get();
      if (c != null && !c.isDisposed()) {
        c.dispose();
        disposedCount++;
      }
    }
    colorMap.clear();
  }
}
