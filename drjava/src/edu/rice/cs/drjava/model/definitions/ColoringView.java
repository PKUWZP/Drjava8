package edu.rice.cs.drjava.model.definitions;

import javax.swing.text.*;
import java.awt.*;
import javax.swing.event.DocumentEvent;
import gj.util.Vector;

import edu.rice.cs.drjava.model.definitions.reducedmodel.*;

/**
 * This view class renders text on the screen using the reduced model info.
 * By extending WrappedPlainView, we only have to override the parts we want to.
 * Here we only override drawUnselectedText. We may want to override 
 * drawSelectedText at some point.
 *
 * @version $Id$
 */
public class ColoringView extends WrappedPlainView {
  private DefinitionsDocument _doc;

  private static final Color COMMENTED_COLOR = Color.green.darker().darker();
  private static final Color DOUBLE_QUOTED_COLOR = Color.red.darker();
  private static final Color SINGLE_QUOTED_COLOR = Color.magenta;
  private static final Color NORMAL_COLOR = Color.black;
  private static final Color KEYWORD_COLOR = Color.blue;

  /**
   * Constructor.
   * @param   Element elem
   */
  ColoringView(Element elem) {
    super(elem);
    _doc = (DefinitionsDocument)getDocument();
  }

  /**
   * Renders the given range in the model as normal unselected
   * text.
   * Note that this is text that's all on one line. The superclass deals
   * with breaking lines and such. So all we have to do here is draw the
   * text on [p0,p1) in the model. We have to start drawing at (x,y), and
   * the function returns the x coordinate when we're done.
   *
   * @param g the graphics context
   * @param x the starting X coordinate
   * @param y the starting Y coordinate
   * @param p0 the beginning position in the model
   * @param p1 the ending position in the model
   * @returns the x coordinate at the end of the range
   * @exception BadLocationException if the range is invalid
   */
  protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1) 
    throws BadLocationException 
  {
    /*
     DrJava.consoleErr().println("drawUnselected: " + p0 + "-" + p1 + 
     " doclen=" + _doc.getLength() +" x="+x+" y="+y);
     */
    // If there's nothing to show, don't do anything!
    // For some reason I don't understand we tend to get called sometimes
    // to render a zero-length area.
    if (p0 == p1) {
      return  x;
    }
    Vector<HighlightStatus> stats = _doc.getHighlightStatus(p0, p1);
    if (stats.size() < 1) {
      throw  new RuntimeException("GetHighlightStatus returned nothing!");
    }
    for (int i = 0; i < stats.size(); i++) {
      HighlightStatus stat = stats.elementAt(i);
      setFormattingForState(g, stat.getState());
      // If this highlight status extends past p1, end at p1
      int length = stat.getLength();
      int location = stat.getLocation();
      if (location + length > p1) {
        length = p1 - stat.getLocation();
      }
      Segment text = getLineBuffer();
      /*
       DrJava.consoleErr().println("Highlight: loc=" + location + " len=" +
       length + " state=" + stat.getState() +
       " text=" + text);
       */
      _doc.getText(location, length, text);
      x = Utilities.drawTabbedText(text, x, y, g, this, location);
    }
    //DrJava.consoleErr().println("returning x: " + x);
    return  x;
  }

  /**
   * put your documentation comment here
   * @param g
   * @param x
   * @param y
   * @param p0
   * @param p1
   * @return 
   * @exception BadLocationException
   */
  protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1) 
    throws BadLocationException 
  {
    /*
     DrJava.consoleErr().println("drawSelected: " + p0 + "-" + p1 + 
     " doclen=" + _doc.getLength() +" x="+x+" y="+y);
     */
    return  super.drawSelectedText(g, x, y, p0, p1);
  }

  /**
   * Given a particular state, assign it a color.
   * @param g Graphics object
   * @param state a given state
   */
  private void setFormattingForState(Graphics g, int state) {
    switch (state) {
      case HighlightStatus.NORMAL:
        g.setColor(NORMAL_COLOR);
        break;
      case HighlightStatus.COMMENTED:
        g.setColor(COMMENTED_COLOR);
        break;
      case HighlightStatus.SINGLE_QUOTED:
        g.setColor(SINGLE_QUOTED_COLOR);
        break;
      case HighlightStatus.DOUBLE_QUOTED:
        g.setColor(DOUBLE_QUOTED_COLOR);
        break;
      case HighlightStatus.KEYWORD:
        g.setColor(KEYWORD_COLOR);
        break;
      default:
        throw  new RuntimeException("Can't get color for invalid state: " + state);
    }
  }

  /**
   * Called when a change occurs.
   * @param changes document changes
   * @param a a Shape
   * @param f a ViewFactory
   */
  public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
    super.changedUpdate(changes, a, f);
    // Make sure we redraw since something changed in the formatting
    getContainer().repaint();
  }
}


