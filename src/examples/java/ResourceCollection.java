import com.readytalk.swt.util.ColorFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Created by stan.steel on 1/13/14.
 */
public class ResourceCollection {
  static Color c;
  public static void main(String[] args) {
    Display display = new Display();
    Shell shell = new Shell(display);
//    Composite composite = new Composite(shell, SWT.NONE);
    final Canvas canvas = new Canvas(shell, SWT.NONE);
    canvas.setSize(700, 700);
    shell.setSize(700, 700);
    shell.layout();

    canvas.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
//        System.out.println("YEE HAW!!!");
//        System.exit(0);

        if (c != null) {
          e.gc.setForeground(c);
        }
        e.gc.setLineWidth(20);
        e.gc.drawRoundRectangle(100, 100, 300, 300, 50, 50);
      }
    });

    new Thread(new Runnable() {
      @Override
      public void run() {

        for(int r = 0; r < 200; r++) {
          for(int g = 0; g < 200; g++) {
            for(int b = 0; b < 200; b++) {
              c = ColorFactory.getColor(r, g, b);
            }
          }
        }
        System.gc();
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();

    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
//        display.sleep();
      }
      canvas.redraw();
    }
    display.dispose();
  }


}
