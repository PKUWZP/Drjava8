/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is a part of DrJava. Current versions of this project are available
 * at http://sourceforge.net/projects/drjava
 *
 * Copyright (C) 2001-2002 JavaPLT group at Rice University (javaplt@rice.edu)
 *
 * DrJava is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrJava is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * or see http://www.gnu.org/licenses/gpl.html
 *
 * In addition, as a special exception, the JavaPLT group at Rice University
 * (javaplt@rice.edu) gives permission to link the code of DrJava with
 * the classes in the gj.util package, even if they are provided in binary-only
 * form, and distribute linked combinations including the DrJava and the
 * gj.util package. You must obey the GNU General Public License in all
 * respects for all of the code used other than these classes in the gj.util
 * package: Dictionary, HashtableEntry, ValueEnumerator, Enumeration,
 * KeyEnumerator, Vector, Hashtable, Stack, VectorEnumerator.
 *
 * If you modify this file, you may extend this exception to your version of the
 * file, but you are not obligated to do so. If you do not wish to
 * do so, delete this exception statement from your version. (However, the
 * present version of DrJava depends on these classes, so you'd want to
 * remove the dependency first!)
 *
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.model.debug;

import junit.framework.*;

import java.io.*;

import javax.swing.text.BadLocationException;
import junit.extensions.*;
import java.util.LinkedList;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.DefaultStyledDocument;
import java.util.Vector;

import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.config.*;
import edu.rice.cs.drjava.model.*;
import edu.rice.cs.drjava.model.definitions.*;
import edu.rice.cs.drjava.model.repl.*;
import edu.rice.cs.drjava.model.compiler.*;
import edu.rice.cs.util.*;
import edu.rice.cs.util.text.DocumentAdapterException;
import edu.rice.cs.drjava.CodeStatus;

/**
 * Tests the JPDA-based debugger.
 *
 * @version $Id$
 */
public final class DebugTest extends DebugTestCase
  implements OptionConstants
{
  
  protected static final String DEBUG_CLASS =
    /*  1 */ "class DrJavaDebugClass {\n" +
    /*  2 */ "  public void foo() {\n" +
    /*  3 */ "    System.out.println(\"Foo Line 1\");\n" +
    /*  4 */ "    bar();\n" +
    /*  5 */ "    System.out.println(\"Foo Line 3\");\n" +
    /*  6 */ "  }\n" +
    /*  7 */ "  public void bar() {\n" +
    /*  8 */ "    System.out.println(\"Bar Line 1\");\n" +
    /*  9 */ "    System.out.println(\"Bar Line 2\");\n" +
    /* 10 */ "  }\n" +
    /* 11 */ "}\n" +
    /* 12 */ "class DrJavaDebugClass2 {\n" +
    /* 13 */ "  public void baz() {\n" +
    /* 14 */ "    System.out.println(\"Baz Line 1\");\n" +
    /* 15 */ "    new DrJavaDebugClass().bar();\n" +
    /* 16 */ "  }\n" +
    /* 17 */ "}";
  
  protected static final String DEBUG_CLASS_WITH_PACKAGE =
    /*  1 */ "package a;\n" +
    /*  2 */ "public class DrJavaDebugClassWithPackage {\n" +
    /*  3 */ "  public void foo() {\n" +
    /*  4 */ "    System.out.println(\"foo line 1\");\n" +
    /*  5 */ "    System.out.println(\"foo line 2\");\n" +
    /*  6 */ "  }\n" +
    /*  7 */ "}";
  
  protected static final String SUSPEND_CLASS =
    "class Suspender {\n" +
    "  public static void main(String[] args) {\n" +
    "    Thread t1 = new Thread(){\n" +
    "      public void run(){\n" +
    "        int a = 1;\n" +
    "        while(true);\n" +
    "      }\n" +
    "    };\n" +
    "    t1.start();\n" +
    "  }\n" +
    "}";
  
  protected static final String MONKEY_CLASS =
    /* 1 */    "class Monkey {\n" +
    /* 2 */    "  public static void main(String[] args) {\n" +
    /* 3 */    "\n" +
    /* 4 */    "    Thread t = new Thread(){\n" +
    /* 5 */    "      public void run(){\n" +
    /* 6 */    "       try{\n" +
    /* 7 */    "         Thread.sleep(1000);\n" +
    /* 8 */    "       }\n" +
    /* 9 */    "       catch(InterruptedException e){\n" +
    /* 10 */    "      }\n" +
    /* 11 */    "      System.out.println(\"I\'m a thread! Yeah!\");\n" +
    /* 12 */    "      }\n" +
    /* 13 */    "    };\n" +
    /* 14 */    "    try{\n" +
    /* 15 */    "      t.start();\n" +
    /* 16 */    "      System.out.println(\"I just woke up.  I\'m a big boy now.\");\n" +
    /* 17 */    "      System.out.println(\"James likes bananas!\");\n" +
    /* 18 */    "      System.out.println(\"Yes they do.\");\n" +
    /* 19 */    "    }catch(Exception e){\n" +
    /* 20 */    "      e.printStackTrace();\n" +
    /* 21 */    "    }\n" +
    /* 22 */    "  }\n" +
    /* 23 */    "}\n";
  
  protected static final String MONKEY_WITH_INNER_CLASS =
    /* 1 */    "class Monkey {\n" +
    /* 2 */    "  static int foo = 6; \n" +
    /* 3 */    "  class MonkeyInner { \n" +
    /* 4 */    "    int innerFoo = 8;\n" +
    /* 5 */    "    class MonkeyInnerInner { \n" +
    /* 6 */    "      int innerInnerFoo = 10;\n" +
    /* 7 */    "      public void innerMethod() { \n" +
    /* 8 */    "        int innerMethodFoo;\n" +
    /* 9 */    "        innerMethodFoo = 12;\n" +
    /* 10 */   "        foo++;\n" +
    /* 11 */   "        innerFoo++;\n" +
    /* 12 */   "        innerInnerFoo++;\n" +
    /* 13 */   "        innerMethodFoo++;\n" +
    /* 14 */   "        staticMethod();\n" +
    /* 15 */   "        System.out.println(\"innerMethodFoo: \" + innerMethodFoo);\n" +
    /* 16 */   "      }\n" +
    /* 17 */   "    }\n" +
    /* 18 */   "  }\n" +
    /* 19 */   "  public void bar() {\n" +
    /* 20 */   "    MonkeyInner.MonkeyInnerInner mi = \n" +
    /* 21 */   "      new MonkeyInner().new MonkeyInnerInner();\n" +
    /* 22 */   "    mi.innerMethod();\n" +
    /* 23 */   "  }\n" +
    /* 24 */   "  public static void staticMethod() {\n" +
    /* 25 */   "    int z = 3;\n" +
    /* 26 */   "  }\n" +
    /* 27 */   "}\n";
  
  protected static final String CLASS_WITH_STATIC_FIELD =
    /*  1 */    "public class DrJavaDebugStaticField {\n" +
    /*  2 */    "  public static int x = 0;\n" +
    /*  3 */    "  public void bar() {\n" +
    /*  4 */    "    System.out.println(\"x == \" + x);\n" +
    /*  5 */    "    x++;\n" +
    /*  6 */    "  }\n" +
    /*  7 */    "  public static void main(String[] nu) {\n" +
    /*  8 */    "    new Thread(\"stuff\") {\n" +
    /*  9 */    "      public void run() {\n" +
    /* 10 */    "        new DrJavaDebugStaticField().bar();\n" +
    /* 11 */    "      }\n" +
    /* 12 */    "    }.start();\n" +
    /* 13 */    "    new DrJavaDebugStaticField().bar();\n" +
    /* 14 */    "  }\n" +
    /* 15 */    "}";
  
  protected static final String MONKEY_STATIC_STUFF =
    /*1*/ "class MonkeyStaticStuff {\n" +
    /*2*/ "  static int foo = 6;\n" +
    /*3*/ "  static class MonkeyInner {\n" +
    /*4*/ "    static int innerFoo = 8;\n" +
    /*5*/ "    static public class MonkeyTwoDeep {\n" +
    /*6*/ "      static int twoDeepFoo = 13;\n" +
    /*7*/ "      static class MonkeyThreeDeep {\n" +
    /*8*/ "        public static int threeDeepFoo = 18;\n" +
    /*9*/ "        public static void threeDeepMethod() {\n" +
    /*10*/"          System.out.println(MonkeyStaticStuff.MonkeyInner.MonkeyTwoDeep.MonkeyThreeDeep.threeDeepFoo);\n" +
    /*11*/"          System.out.println(MonkeyTwoDeep.twoDeepFoo);\n" +
    /*12*/"          System.out.println(MonkeyStaticStuff.foo);\n" +
    /*13*/"          System.out.println(MonkeyStaticStuff.MonkeyInner.innerFoo);\n" +
    /*14*/"          System.out.println(MonkeyInner.MonkeyTwoDeep.twoDeepFoo);\n" +
    /*15*/"          System.out.println(innerFoo);\n" +
    /*16*/"        }\n" +
    /*17*/"      }\n" +
    /*18*/"      static int getNegativeTwo() { return -2; }\n" +    
    /*19*/"    }\n" +
    /*20*/"  }\n" +
    /*21*/"}";
  
  
  /**
   * Constructor.
   * @param  String name
   */
  public DebugTest(String name) {
    super(name);
  }
  
  
  /**
   * Tests startup and shutdown, ensuring that all appropriate fields are
   * initialized.
   */
  public void testStartupAndShutdown()
    throws DebugException, InterruptedException
  {
    if (printMessages) System.out.println("----testStartupAndShutdown----");
    DebugTestListener debugListener = new DebugStartAndStopListener();
    _debugger.addListener(debugListener);
    
    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    debugListener.assertDebuggerStartedCount(1);
    debugListener.assertDebuggerShutdownCount(0);

    // Check fields and status
    assertTrue("Debug Manager should be ready", _debugger.isReady());
    assertNotNull("EventRequestManager should not be null after startup",
                  _debugger.getEventRequestManager());
    assertNotNull("PendingRequestManager should not be null after startup",
                  _debugger.getPendingRequestManager());
    
    // Shutdown the debugger
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(1);  // shutdown
      _notifierLock.wait();
    }
    debugListener.assertDebuggerShutdownCount(1);
    _debugger.removeListener(debugListener);
  }
  
  /**
   * Test that when two threads are suspended setCurrentThread can be used
   * to switch between them in the debugger
   */
  public synchronized void testMultiThreadedSetCurrentThread()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) System.out.println("----testMultiThreadedSetCurrentThread----");
    BreakpointTestListener debugListener = new BreakpointTestListener();
    
     // Compile the class
     OpenDefinitionsDocument doc = doCompile(MONKEY_CLASS, tempFile());
     _debugger.addListener(debugListener);
     // Start debugger
     synchronized(_notifierLock) {
       _debugger.startup();
       _waitForNotifies(1);  // startup
       _notifierLock.wait();
     }
     
     // Set two breakpoints
     int index = MONKEY_CLASS.indexOf("System.out.println(\"I\'m a thread! Yeah!\");");
     _debugger.toggleBreakpoint(doc,index,11);
     index = MONKEY_CLASS.indexOf("System.out.println(\"James likes bananas!\");");
     _debugger.toggleBreakpoint(doc,index,17);
     
      // Run the main() method, hitting both breakpoints in different threads
     synchronized(_notifierLock) {
       interpretIgnoreResult("java Monkey");
       _waitForNotifies(6); // (suspended, updated, breakpointReached) * 2
       _notifierLock.wait();
     }
     DebugThreadData threadA = new DebugThreadData(_debugger.getCurrentThread());
     DebugThreadData threadB = new DebugThreadData(_debugger.getThreadAt(1));
     synchronized(_notifierLock) {
       _asyncDoSetCurrentThread(threadB);
       _waitForNotifies(2);  // updated, suspended
       _notifierLock.wait();
     }
     
     DebugThreadData thread1 = new DebugThreadData(_debugger.getThreadAt(1));
     DebugThreadData thread2 = new DebugThreadData(_debugger.getCurrentThread());
     
     // make sure threads have switched places
     assertTrue(thread1.getUniqueID() == threadA.getUniqueID());
     assertTrue(thread2.getUniqueID() == threadB.getUniqueID());
     
       // Close doc and make sure breakpoints are removed
     _model.closeFile(doc);
     
     // Shutdown the debugger
     if (printMessages) System.out.println("Shutting down...");
     InterpretListener interpretListener = new InterpretListener() {
       public void interpreterChanged(boolean inProgress){
         // Don't notify: happens in the same thread
        interpreterChangedCount++;
       }
     };
     _model.addListener(interpretListener);
     synchronized(_notifierLock) {
       //_asyncResume();
       //_asyncResume();
       _debugger.shutdown();
       _waitForNotifies(2);  // shutdown, interactionEnded
       _notifierLock.wait();
     }
     interpretListener.assertInterpreterChangedCount(1);
     debugListener.assertDebuggerShutdownCount(1);  //fires
     if (printMessages) System.out.println("Shut down.");
     _model.removeListener(interpretListener);
     _debugger.removeListener(debugListener);
  }

  /**
   * This test has been commented out because we do not support setting the
   * current thread to be an unsuspended thread right now
   */
//  /**
//   * Tests that setCurrentThread works for multiple threads
//   */
//   public synchronized void testMultiThreadedSetCurrentThread()
//     throws DebugException, BadLocationException, DocumentAdapterException,
//     IOException, InterruptedException
//   {
//     if (printMessages) System.out.println("----testMultiThreadedSetCurrentThread----");
//     BreakpointTestListener debugListener = new BreakpointTestListener();
//
//     // Compile the class
//     OpenDefinitionsDocument doc = doCompile(SUSPEND_CLASS, tempFile());
//     _debugger.addListener(debugListener);
//     // Start debugger
//     synchronized(_notifierLock) {
//       _debugger.startup();
//       _waitForNotifies(1);
//       _notifierLock.wait();
//     }
//     debugListener.assertDebuggerStartedCount(1);
//     debugListener.assertDebuggerShutdownCount(0);
//     assertTrue("Debug Manager should be ready", _debugger.isReady());
//
//     int index = SUSPEND_CLASS.indexOf("int a = 1;");
//     _debugger.toggleBreakpoint(doc,index,5);
//
//      // Run the main() method, hitting breakpoints
//     synchronized(_notifierLock) {
//       interpretIgnoreResult("java Suspender");
//       _waitForNotifies(3); // suspended, updated, breakpointReached
//       _notifierLock.wait();
//     }
//     final DebugThreadData thread = new DebugThreadData(_debugger.getCurrentThread());
//     synchronized(_notifierLock){
//       /** _debugger.setCurrentThread(...);
//        * must be executed in another thread because otherwise the notifies
//        * will be received before the _notifierLock is released
//        */
//       new Thread() {
//         public void run(){
//           try{
//             _debugger.resume();
//             _doSetCurrentThread(thread);
//           }catch(DebugException excep){
//             excep.printStackTrace();
//             fail("_doSetCurrentThread failed in testMultiThreadedSetCurrentThread");
//           }
//         }
//       }.start();
//       _waitForNotifies(2);  // suspended, updated
//       _notifierLock.wait();
//     }
//     // Ensure thread suspended
//     debugListener.assertCurrThreadSuspendedCount(2);  //fires
//
//       // Close doc and make sure breakpoints are removed
//     _model.closeFile(doc);
//
//     // Shutdown the debugger
//     if (printMessages) System.out.println("Shutting down...");
//     synchronized(_notifierLock) {
//       _debugger.shutdown();
//       _waitForNotifies(1);  // shutdown
//       _notifierLock.wait();
//     }
//
//     debugListener.assertDebuggerShutdownCount(1);  //fires
//     if (printMessages) System.out.println("Shut down.");
//     _debugger.removeListener(debugListener);
//   }
  
  
  /**
   * Tests that breakpoints behave correctly for multiple threads
   */
  public synchronized void testMultiThreadedBreakpointsAndStep()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) System.out.println("----testMultiThreadedBreakpointsAndStep----");
    BreakpointTestListener debugListener = new BreakpointTestListener();
    
    // Compile the class
    OpenDefinitionsDocument doc = doCompile(MONKEY_CLASS, tempFile());
    
    _debugger.addListener(debugListener);
    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    
    // Set breakpoints
    int index = MONKEY_CLASS.indexOf("System.out.println(\"I\'m a thread! Yeah!\");");
    _debugger.toggleBreakpoint(doc,index,11);
    index = MONKEY_CLASS.indexOf("System.out.println(\"I just woke up.  I\'m a big boy now.\");");
    _debugger.toggleBreakpoint(doc,index,16);
    debugListener.assertBreakpointSetCount(2);
    
    // Run the main() method, hitting breakpoints
    synchronized(_notifierLock) {
      interpretIgnoreResult("java Monkey");
      _waitForNotifies(6);  // (suspended, updated, breakpointReached) x 2
      _notifierLock.wait();
    }
    
    DebugThreadData thread = new DebugThreadData(_debugger.getCurrentThread());
    // Resumes one thread, finishing it and switching to the next break point
    synchronized(_notifierLock) {
      _asyncResume();
      _waitForNotifies(3);  // currThreadDied, suspended, updated
      _notifierLock.wait();
    }
    
    DebugThreadData thread2 = new DebugThreadData(_debugger.getCurrentThread());
    assertTrue("testMultiThreadedBreakPoint thread references should not be equal",
               !thread.getName().equals(thread2.getName()));
    
    // Ensure breakpoint is hit
    debugListener.assertBreakpointReachedCount(2);  //fires
    debugListener.assertThreadLocationUpdatedCount(3);  //fires
    debugListener.assertCurrThreadSuspendedCount(3);  //fires
    debugListener.assertCurrThreadResumedCount(1);
    debugListener.assertCurrThreadDiedCount(1);
    _debugger.removeListener(debugListener);
    
    if( printMessages ) System.out.println("Testing stepping...");
    // Step
    StepTestListener stepTestListener = new StepTestListener();
    _debugger.addListener(stepTestListener);
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_INTO);
      _waitForNotifies(2); // suspended, updated
      _notifierLock.wait();
    }
    stepTestListener.assertStepRequestedCount(1);
    _debugger.removeListener(stepTestListener);
    
    DebugThreadData thread3 = new DebugThreadData(_debugger.getCurrentThread());
    assertEquals("testMultiThreadedBreakPoint thread references should be equal",
                 thread2.getName(), thread3.getName());
    
    // Resume until finished, waiting for interpret call to end
    _debugger.addListener(debugListener);
    InterpretListener interpretListener = new InterpretListener();
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _asyncResume();
      _waitForNotifies(3);  // interactionEnded, currThreadDied, interpreterChanged
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);
    
    // Close doc
    _model.closeFile(doc);
    
    // Shutdown the debugger
    if (printMessages) System.out.println("Shutting down...");
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(1);  // shutdown
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("Shut down.");
  }
  
  
  
  /**
   * Tests that breakpoints behave correctly.
   */
  public synchronized void testBreakpoints()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) System.out.println("----testBreakpoints----");
    BreakpointTestListener debugListener = new BreakpointTestListener();
    
    // Compile the class
    OpenDefinitionsDocument doc = doCompile(DEBUG_CLASS, tempFile());
    _debugger.addListener(debugListener);
    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);
      _notifierLock.wait();
    }
    
   // Add breakpoint before class is loaded
    _debugger.toggleBreakpoint(doc,DEBUG_CLASS.indexOf("bar();"),4);
    debugListener.assertBreakpointSetCount(1);
    
    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("new DrJavaDebugClass().foo()");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("----After breakpoint:\n" + getInteractionsText());
      
    // Ensure breakpoint is hit
    debugListener.assertBreakpointReachedCount(1);  //fires
    debugListener.assertThreadLocationUpdatedCount(1);  //fires
    debugListener.assertCurrThreadSuspendedCount(1);  //fires
    debugListener.assertCurrThreadResumedCount(0);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsContains("Foo Line 1");
    assertInteractionsDoesNotContain("Bar Line 1");
    
    if (printMessages) System.out.println("adding another breakpoint");
    
    // Set another breakpoint (after is class loaded)
    _debugger.toggleBreakpoint(doc,
       DEBUG_CLASS.indexOf("System.out.println(\"Bar Line 2\")"), 9);
    debugListener.assertBreakpointSetCount(2);
    
    
    // Resume until next breakpoint
    synchronized(_notifierLock) {
      if (printMessages) System.out.println("resuming");
      _asyncResume();
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    if (printMessages) System.out.println("----After one resume:\n" + getInteractionsText());
    debugListener.assertCurrThreadResumedCount(1);  //fires (no waiting)
    debugListener.assertBreakpointReachedCount(2);  //fires
    debugListener.assertThreadLocationUpdatedCount(2);  //fires
    debugListener.assertCurrThreadSuspendedCount(2);  //fires
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsContains("Bar Line 1");
    assertInteractionsDoesNotContain("Bar Line 2");
    
    // Resume until finished, waiting for interpret call to end
    InterpretListener interpretListener = new InterpretListener();
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      if( printMessages ) System.err.println("-------- Resuming --------");
      _asyncResume();
      _waitForNotifies(3);  // interactionEnded, currThreadDied, interpreterChanged
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);
    
    if (printMessages) System.out.println("----After second resume:\n" + getInteractionsText());
    debugListener.assertCurrThreadResumedCount(2);  //fires (no waiting)
    debugListener.assertCurrThreadDiedCount(1);  //fires
    debugListener.assertBreakpointReachedCount(2);
    debugListener.assertThreadLocationUpdatedCount(2);
    debugListener.assertCurrThreadSuspendedCount(2);
    assertInteractionsContains("Foo Line 3");
    
    // Close doc and make sure breakpoints are removed
    _model.closeFile(doc);
    debugListener.assertBreakpointRemovedCount(2);  //fires twice (no waiting)
      
    // Shutdown the debugger
    if (printMessages) System.out.println("Shutting down...");
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(1);  // shutdown
      _notifierLock.wait();
    }
    
    debugListener.assertDebuggerShutdownCount(1);  //fires
    if (printMessages) System.out.println("Shut down.");
    _model.removeListener(interpretListener);
    _debugger.removeListener(debugListener);
  }
  
  /**
   * Tests that breakpoints and steps behave correctly.
   */
  public void testStepInto()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) System.out.println("----testStepInto----");
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    OpenDefinitionsDocument doc = doCompile(DEBUG_CLASS, tempFile());
   
    _debugger.addListener(debugListener);
    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    
    // Add a breakpoint
    _debugger.toggleBreakpoint(doc,DEBUG_CLASS.indexOf("bar();"),4);
    debugListener.assertBreakpointSetCount(1);
    
    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("new DrJavaDebugClass().foo()");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("----After breakpoint:\n" + getInteractionsText());
      
    // Ensure breakpoint is hit
    debugListener.assertBreakpointReachedCount(1);  //fires
    debugListener.assertThreadLocationUpdatedCount(1);  //fires
    debugListener.assertCurrThreadSuspendedCount(1);  //fires
    debugListener.assertCurrThreadResumedCount(0);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsContains("Foo Line 1");
    assertInteractionsDoesNotContain("Bar Line 1");

    // Step into bar() method
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_INTO);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(1); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(2);  // fires
    debugListener.assertCurrThreadSuspendedCount(2);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsDoesNotContain("Bar Line 1");
    
    // Step to next line
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("****"+getInteractionsText());
    debugListener.assertStepRequestedCount(2);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(2); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(3);  // fires
    debugListener.assertCurrThreadDiedCount(0);
    debugListener.assertCurrThreadSuspendedCount(3);  // fires
    debugListener.assertBreakpointReachedCount(1);
    assertInteractionsContains("Bar Line 1");
    assertInteractionsDoesNotContain("Bar Line 2");
    
    // Step to next line
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(3);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(3); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(4);  // fires
    debugListener.assertCurrThreadDiedCount(0);
    debugListener.assertCurrThreadSuspendedCount(4);  // fires
    debugListener.assertBreakpointReachedCount(1);
    assertInteractionsContains("Bar Line 2");
    assertInteractionsDoesNotContain("Foo Line 3");
    
    // Step twice to print last line in Foo
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(5);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(5); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(6);  // fires
    debugListener.assertCurrThreadDiedCount(0);
    debugListener.assertCurrThreadSuspendedCount(6);  //fires
    debugListener.assertBreakpointReachedCount(1);
    assertInteractionsContains("Foo Line 3");
    
    
    // Step again to finish, waiting for interpret call to end
    InterpretListener interpretListener = new InterpretListener();
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(3);  // interactionEnded, currThreadDied, interpreterChanged
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);
    
    debugListener.assertStepRequestedCount(6);  // fires (don't wait)
    debugListener.assertCurrThreadDiedCount(1);

    // Shutdown the debugger
    if (printMessages) System.out.println("Shutting down...");
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(1);  // shutdown
      _notifierLock.wait();
    }
  
    debugListener.assertBreakpointRemovedCount(1);  //fires once (no waiting)
    debugListener.assertDebuggerShutdownCount(1);  //fires
    if (printMessages) System.out.println("Shut down.");
    _debugger.removeListener(debugListener);
  }
  
  /**
   * Tests that stepping out of a method behaves correctly.
   */
  public synchronized void testStepOut()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages)  System.out.println("----testStepOut----");
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    File file2 = new File(_tempDir, "DrJavaDebugClass.java");
    OpenDefinitionsDocument doc = doCompile(DEBUG_CLASS, file2);
    _debugger.addListener(debugListener);
    // Start debugger and add breakpoint
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    
    // Set breakpoint
    _debugger.toggleBreakpoint(doc,DEBUG_CLASS.indexOf("bar();"),4);
    debugListener.assertBreakpointSetCount(1);
    
    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("new DrJavaDebugClass().foo()");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("----After breakpoint:\n" + getInteractionsText());
      
    // Ensure breakpoint is hit
    debugListener.assertBreakpointReachedCount(1);  // fires
    debugListener.assertThreadLocationUpdatedCount(1);  // fires
    debugListener.assertCurrThreadSuspendedCount(1);  // fires
    debugListener.assertCurrThreadResumedCount(0);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsContains("Foo Line 1");
    assertInteractionsDoesNotContain("Bar Line 1");

    // Step into bar() method
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_INTO);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(1); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(2);  //fires
    debugListener.assertCurrThreadSuspendedCount(2);  //fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsDoesNotContain("Bar Line 1");
    
    // Step out of method
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OUT);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("****"+getInteractionsText());
    debugListener.assertStepRequestedCount(2);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(2); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(3);  // fires
    debugListener.assertCurrThreadDiedCount(0);
    debugListener.assertCurrThreadSuspendedCount(3);  //fires
    debugListener.assertBreakpointReachedCount(1);
    assertInteractionsContains("Bar Line 2");
    assertInteractionsDoesNotContain("Foo Line 3");
    
    // Shutdown the debugger and listen for the interpret call to end
    if (printMessages) System.out.println("Shutting down...");
    InterpretListener interpretListener = new InterpretListener() {
       public void interpreterChanged(boolean inProgress) {
         // Don't notify: happens in the same thread
        interpreterChangedCount++;
       }
     };
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(2);  // interactionEnded, shutdown
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);

    debugListener.assertBreakpointRemovedCount(1);  // fires (don't wait)
    debugListener.assertDebuggerShutdownCount(1);  // fires
    if (printMessages) System.out.println("Shut down.");
    _debugger.removeListener(debugListener);
  }
  
  /**
   * Tests that stepping works in a public class with a package
   */
  public synchronized void testStepOverWithPackage()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) System.out.println("----testStepOverWithPackage----");
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    File aDir = new File(_tempDir, "a");
    aDir.mkdir();
    File file = new File(aDir, "DrJavaDebugClassWithPackage.java");
    OpenDefinitionsDocument doc = doCompile(DEBUG_CLASS_WITH_PACKAGE, file);
    
    _debugger.addListener(debugListener);
    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    
    // Add a breakpoint
    _debugger.toggleBreakpoint(doc,DEBUG_CLASS_WITH_PACKAGE.indexOf("foo line 1"), 4);
    debugListener.assertBreakpointSetCount(1);
    
    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("new a.DrJavaDebugClassWithPackage().foo()");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("----After breakpoint:\n" + getInteractionsText());
      
    // Ensure breakpoint is hit
    debugListener.assertBreakpointReachedCount(1);  //fires
    debugListener.assertThreadLocationUpdatedCount(1);  //fires
    debugListener.assertCurrThreadSuspendedCount(1);  //fires
    debugListener.assertCurrThreadResumedCount(0);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsDoesNotContain("foo line 1");

    // Step over once
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(1); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(2);  // fires
    debugListener.assertCurrThreadSuspendedCount(2);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsContains("foo line 1");
    assertInteractionsDoesNotContain("foo line 2");
    
    // Step over again
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("****"+getInteractionsText());
    debugListener.assertStepRequestedCount(2);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(2); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(3);  // fires
    debugListener.assertCurrThreadDiedCount(0);
    debugListener.assertCurrThreadSuspendedCount(3);  // fires
    debugListener.assertBreakpointReachedCount(1);
    assertInteractionsContains("foo line 2");
    
    // Resume until finished, waiting for interpret call to finish
    InterpretListener interpretListener = new InterpretListener();
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _asyncResume();
      _waitForNotifies(3);  // interactionEnded, currThreadDied, interpreterChanged
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);

    if (printMessages) System.out.println("----After resume:\n" + getInteractionsText());
    debugListener.assertCurrThreadResumedCount(3);  //fires (no waiting)
    debugListener.assertCurrThreadDiedCount(1);  //fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertThreadLocationUpdatedCount(3);
    debugListener.assertCurrThreadSuspendedCount(3);

   
    // Close doc and make sure breakpoints are removed
    _model.closeFile(doc);
    debugListener.assertBreakpointRemovedCount(1);  //fires (no waiting)
    
    // Shutdown the debugger
    if (printMessages) System.out.println("Shutting down...");
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(1);  // shutdown
      _notifierLock.wait();
    }
    
    debugListener.assertDebuggerShutdownCount(1);  //fires
    if (printMessages) System.out.println("Shut down.");
    _debugger.removeListener(debugListener);
  }
  
  /**
   * Tests that the sourcepath config option properly adds files to the
   * search directories.
   */
  public void testDebugSourcepath()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages)  System.out.println("----testDebugSourcePath----");
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    File file2 = new File(_tempDir, "DrJavaDebugClass.java");
    OpenDefinitionsDocument doc = doCompile(DEBUG_CLASS, file2);
    Vector<File> path = new Vector<File>();
    path.addElement(_tempDir);
    
    _debugger.addListener(debugListener);
  
    // Start debugger and add breakpoint
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    _debugger.toggleBreakpoint(doc,DEBUG_CLASS.indexOf("bar();"),4);
   
    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("new DrJavaDebugClass().foo()");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    // Source is highlighted because document is stored in breakpoint
    debugListener.assertThreadLocationUpdatedCount(1);  // fires
    
    // Step into bar() method
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_INTO);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    // Source is highlighted because file is in source root set
    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(2);  // fires
    
    
    // Close file so it won't be in source root set
    _model.closeFile(doc);
    debugListener.assertBreakpointRemovedCount(1);
    
    // Step to next line
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(1);  // suspended
      _notifierLock.wait();
    }
    // Source is not highlighted
    debugListener.assertStepRequestedCount(2);  // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(2);  // doesn't fire
   
    synchronized(_debugger) {
      // Add _tempDir to our sourcepath
      DrJava.getConfig().setSetting(OptionConstants.DEBUG_SOURCEPATH, path);
    }
    
    // Step to next line
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    // Source is highlighted because file is now on sourcepath
    debugListener.assertStepRequestedCount(3);  // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(3);  // fires
    
    // Shutdown the debugger and listen for the interpret call to end
    if (printMessages) System.out.println("Shutting down...");
    InterpretListener interpretListener = new InterpretListener() {
       public void interpreterChanged(boolean inProgress) {
         // Don't notify: happens in the same thread
        interpreterChangedCount++;
       }
     };
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(2);  // interactionEnded, shutdown
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);  // fires
    interpretListener.assertInterpreterChangedCount(1);  // fires (don't wait)
    _model.removeListener(interpretListener);
    
    debugListener.assertDebuggerShutdownCount(1);  // fires
    if (printMessages) System.out.println("Shut down.");
    _debugger.removeListener(debugListener);
  }
  
  /**
   * Tests that breakpoints behave correctly in non-public classes.
   */
  public synchronized void testBreakpointsAndStepsInNonPublicClasses()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) System.out.println("----testBreakpointsAndStepsInNonPublicClasses----");
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    OpenDefinitionsDocument doc = doCompile(DEBUG_CLASS, tempFile());
    _debugger.addListener(debugListener);
    // Start debugger and add breakpoint (before class is loaded)
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);
      _notifierLock.wait();
    }

    // Add a breakpoint
    _debugger.toggleBreakpoint(doc,DEBUG_CLASS.indexOf("Baz Line 1"),14);
    debugListener.assertBreakpointSetCount(1);
    
    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("new DrJavaDebugClass2().baz()");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("----After breakpoint:\n" + getInteractionsText());
    
    // Ensure breakpoint is hit
    debugListener.assertBreakpointReachedCount(1);  //fires
    debugListener.assertThreadLocationUpdatedCount(1);  //fires
    debugListener.assertCurrThreadSuspendedCount(1);  //fires
    debugListener.assertCurrThreadResumedCount(0);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsDoesNotContain("Baz Line 1");
    
    if (printMessages) System.out.println("adding another breakpoint");
    
    // Set another breakpoint (after is class loaded)
    _debugger.toggleBreakpoint(doc,
       DEBUG_CLASS.indexOf("System.out.println(\"Bar Line 2\")"), 9);
    debugListener.assertBreakpointSetCount(2);
    
    // Step to next line
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    
    if (printMessages) System.out.println("****"+getInteractionsText());
    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(1); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(2); // fires
    debugListener.assertCurrThreadDiedCount(0);
    debugListener.assertCurrThreadSuspendedCount(2);  //fires
    debugListener.assertBreakpointReachedCount(1);
    assertInteractionsContains("Baz Line 1");
    assertInteractionsDoesNotContain("Bar Line 1");
    
    // Resume until next breakpoint
    synchronized(_notifierLock) {
      if (printMessages) System.out.println("resuming");
      _asyncResume();
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    if (printMessages) System.out.println("----After one resume:\n" + getInteractionsText());
    debugListener.assertCurrThreadResumedCount(2);  //fires (no waiting)
    debugListener.assertBreakpointReachedCount(2);  //fires
    debugListener.assertThreadLocationUpdatedCount(3);  //fires
    debugListener.assertCurrThreadSuspendedCount(3);  //fires
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsContains("Bar Line 1");
    assertInteractionsDoesNotContain("Bar Line 2");

    if( printMessages ) System.err.println("-------- Adding interpret listener --------");
    // Resume until finished, waiting for call to interpret to end
    InterpretListener interpretListener = new InterpretListener();
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      if( printMessages ) System.err.println("-------- resuming --------");
      _asyncResume();
      _waitForNotifies(3);  // currThreadDied, interactionEnded, interpreterChanged
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);
    
    if (printMessages) System.out.println("----After second resume:\n" + getInteractionsText());
    debugListener.assertCurrThreadResumedCount(3);  //fires (no waiting)
    debugListener.assertCurrThreadDiedCount(1);  //fires
    debugListener.assertBreakpointReachedCount(2);
    debugListener.assertThreadLocationUpdatedCount(3);
    debugListener.assertCurrThreadSuspendedCount(3);
    assertInteractionsContains("Bar Line 2");
    
    // Close doc and make sure breakpoints are removed
    _model.closeFile(doc);
    debugListener.assertBreakpointRemovedCount(2);  //fires twice (no waiting)
      
    // Shutdown the debugger
    if (printMessages) System.out.println("Shutting down...");
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(1);  // shutdown
      _notifierLock.wait();
    }
    debugListener.assertDebuggerShutdownCount(1);  //fires
    if (printMessages) System.out.println("Shut down.");
    _debugger.removeListener(debugListener);
  }
  
  
  /**
   * Tests the utility function to get a relative directory for a package.
   */
  public void testGetPackageDir() {
    String class1 = "edu.rice.cs.drjava.model.MyTest";
    String class2 = "MyTest";
    String sep = System.getProperty("file.separator");
    
    assertEquals("package dir with package",
                 "edu" + sep + "rice" + sep + "cs" + sep +
                 "drjava" + sep + "model" + sep,
                 _debugger.getPackageDir(class1));
    assertEquals("package dir without package",
                 "",
                 _debugger.getPackageDir(class2));
  }
  
  /**
   * Tests that stepping into a breakpoint works.
   */
  public synchronized void testStepIntoOverBreakpoint()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) {
      System.out.println("----testStepIntoOverBreakpoint----");
    }
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    File file = new File(_tempDir, "DrJavaDebugClass.java");
    OpenDefinitionsDocument doc = doCompile(DEBUG_CLASS, file);
    
    _debugger.addListener(debugListener);
    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    debugListener.assertDebuggerStartedCount(1);
    
    // Add a breakpoint
    _debugger.toggleBreakpoint(doc,DEBUG_CLASS.indexOf("Foo Line 1"), 3);
    _debugger.toggleBreakpoint(doc,DEBUG_CLASS.indexOf("bar();\n"), 4);
    debugListener.assertBreakpointSetCount(2);
    
    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("new DrJavaDebugClass().foo()");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    
    if (printMessages) {
      System.out.println("----After breakpoint:\n" + getInteractionsText());
    }
      
    // Ensure breakpoint is hit
    debugListener.assertBreakpointReachedCount(1);  //fires
    debugListener.assertThreadLocationUpdatedCount(1);  //fires
    debugListener.assertCurrThreadSuspendedCount(1);  //fires
    debugListener.assertCurrThreadResumedCount(0);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsDoesNotContain("Foo Line 1");

    // Step over once
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(1); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(2);  // fires
    debugListener.assertCurrThreadSuspendedCount(2);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsContains("Foo Line 1");
    
    // Step over again
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    
    if (printMessages) {
      System.out.println("****"+getInteractionsText());
    }
    debugListener.assertStepRequestedCount(2);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(2); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(3);  // fires
    debugListener.assertCurrThreadDiedCount(0);
    debugListener.assertCurrThreadSuspendedCount(3);  // fires
    debugListener.assertBreakpointReachedCount(1);
    
    // Resume until finished, waiting for interpret call to finish
    InterpretListener interpretListener = new InterpretListener();
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _asyncResume();
      _waitForNotifies(3);  // interactionEnded, currThreadDied, interpreterChanged
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);

    if (printMessages) {
      System.out.println("----After resume:\n" + getInteractionsText());
    }
    debugListener.assertCurrThreadResumedCount(3);  //fires (no waiting)
    debugListener.assertCurrThreadDiedCount(1);  //fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertThreadLocationUpdatedCount(3);
    debugListener.assertCurrThreadSuspendedCount(3);

   
    // Close doc and make sure breakpoints are removed
    _model.closeFile(doc);
    debugListener.assertBreakpointRemovedCount(2);  //fires (no waiting)
    
    // Shutdown the debugger
    if (printMessages) {
      System.out.println("Shutting down...");
    }
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(1);  // shutdown
      _notifierLock.wait();
    }
    
    debugListener.assertDebuggerShutdownCount(1);  //fires
    if (printMessages) {
      System.out.println("Shut down.");
    }
    _debugger.removeListener(debugListener);
  }

  /**
   * Tests that static fields are consistent across different interpreter contexts.
   */
  public void testStaticFieldsConsistent()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) {
      System.out.println("----testStaticFieldsConsistent----");
    }
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    File file = new File(_tempDir, "DrJavaDebugStaticField.java");
    OpenDefinitionsDocument doc = doCompile(CLASS_WITH_STATIC_FIELD, file);
    
    _debugger.addListener(debugListener);

    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    debugListener.assertDebuggerStartedCount(1);
    
    _debugger.toggleBreakpoint(doc,CLASS_WITH_STATIC_FIELD.indexOf("System.out.println"), 4);
    debugListener.assertBreakpointSetCount(1);

    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("java DrJavaDebugStaticField");
      _waitForNotifies(6);  // (suspended, updated, breakpointReached) *2
      _notifierLock.wait();
    }

    DebugThreadData threadA = new DebugThreadData(_debugger.getCurrentThread());
    DebugThreadData threadB = new DebugThreadData(_debugger.getThreadAt(1));

     if (printMessages) {
      System.out.println("----After breakpoint:\n" + getInteractionsText());
    }
      
    // Ensure breakpoint is hit
    debugListener.assertBreakpointReachedCount(2);  //fires
    debugListener.assertThreadLocationUpdatedCount(2);  //fires
    debugListener.assertCurrThreadSuspendedCount(2);  //fires
    debugListener.assertCurrThreadResumedCount(0);
    debugListener.assertCurrThreadDiedCount(0);
    assertEquals("x has correct value at start", "0", interpret("DrJavaDebugStaticField.x"));
    assertEquals("assigning x succeeds", "5", interpret("DrJavaDebugStaticField.x = 5"));
    assertEquals("assignment reflected in this", "5", interpret("this.x"));

    // Step over once
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(1); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(3);  // fires
    debugListener.assertCurrThreadSuspendedCount(3);  // fires
    debugListener.assertBreakpointReachedCount(2);
    debugListener.assertCurrThreadDiedCount(0);
    assertInteractionsContains("x == 5");
    assertEquals("x retains correct value after step", "5", interpret("DrJavaDebugStaticField.x"));
    assertEquals("this has correct value for x after step", "5", interpret("this.x"));

    // Step over again
    synchronized(_notifierLock) {
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    
    if (printMessages) {
      System.out.println("****"+getInteractionsText());
    }
    debugListener.assertStepRequestedCount(2);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(2); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(4);  // fires
    debugListener.assertCurrThreadDiedCount(0);
    debugListener.assertCurrThreadSuspendedCount(4);  // fires
    debugListener.assertBreakpointReachedCount(2);
    assertEquals("x has correct value after increment", "6", interpret("DrJavaDebugStaticField.x"));
    assertEquals("this has correct value for x after increment", "6", interpret("this.x"));

    synchronized(_notifierLock){
      _asyncDoSetCurrentThread(threadB);
      _waitForNotifies(2);  // updated, suspended
      _notifierLock.wait();
    }
    assertEquals("x has correct value in other thread", "6", interpret("DrJavaDebugStaticField.x"));
    assertEquals("this has correct value for x in other thread", "6", interpret("this.x"));

    // Close doc and make sure breakpoints are removed
    _model.closeFile(doc);
    debugListener.assertBreakpointRemovedCount(1);  //fires (no waiting)
    
    // Shutdown the debugger
    if (printMessages) {
      System.out.println("Shutting down...");
    }
    InterpretListener interpretListener = new InterpretListener() {
       public void interpreterChanged(boolean inProgress) {
         // Don't notify: happens in the same thread
        interpreterChangedCount++;
       }
     };
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(2);  // interactionEnded, shutdown
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);
    
    debugListener.assertDebuggerShutdownCount(1);  //fires
    if (printMessages) {
      System.out.println("Shut down.");
    }
    _debugger.removeListener(debugListener);
  }

  /**
   * Tests that watches can correctly see the values of local
   * variables, fields and fields of outer classes.
   */
  public void testNonStaticWatches()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) {
      System.out.println("----testNonStaticWatches----");
    }
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    File file = new File(_tempDir, "Monkey.java");
    OpenDefinitionsDocument doc = doCompile(MONKEY_WITH_INNER_CLASS, file);
    
    _debugger.addListener(debugListener);

    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    debugListener.assertDebuggerStartedCount(1);
    
    _debugger.toggleBreakpoint(doc,MONKEY_WITH_INNER_CLASS.indexOf("innerMethodFoo = 12;"), 9);
    debugListener.assertBreakpointSetCount(1);

    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("new Monkey().new MonkeyInner().new MonkeyInnerInner().innerMethod()");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    _debugger.addWatch("foo");
    _debugger.addWatch("innerFoo");
    _debugger.addWatch("innerInnerFoo");
    _debugger.addWatch("innerMethodFoo");
    _debugger.addWatch("asdf");
    
    if (printMessages) {
      System.out.println("first step");
    }
    // Step to line 10
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(1); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(2);  // fires
    debugListener.assertCurrThreadSuspendedCount(2);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    
    Vector<DebugWatchData> watches = _debugger.getWatches();
    assertEquals("watch name incorrect", "foo", watches.elementAt(0).getName());
    assertEquals("watch name incorrect", "innerFoo", watches.elementAt(1).getName());
    assertEquals("watch name incorrect", "innerInnerFoo", watches.elementAt(2).getName());
    assertEquals("watch name incorrect", "innerMethodFoo", watches.elementAt(3).getName());
    assertEquals("watch name incorrect", "asdf", watches.elementAt(4).getName());
    assertEquals("watch value incorrect", "6", watches.elementAt(0).getValue());
    assertEquals("watch value incorrect", "8", watches.elementAt(1).getValue());
    assertEquals("watch value incorrect", "10", watches.elementAt(2).getValue());
    assertEquals("watch value incorrect", "12", watches.elementAt(3).getValue());
    assertEquals("watch value incorrect", DebugWatchData.NO_VALUE, watches.elementAt(4).getValue());

    interpret("innerFoo = 0");
    watches = _debugger.getWatches();
    assertEquals("watch name incorrect", "innerFoo", watches.elementAt(1).getName());
    assertEquals("watch value incorrect", "0", watches.elementAt(1).getValue());

    interpret("innerFoo = 8");
    assertEquals("watch name incorrect", "innerFoo", watches.elementAt(1).getName());
    assertEquals("watch value incorrect", "8", watches.elementAt(1).getValue());

    if (printMessages) {
      System.out.println("second step");
    }
    // Step to line 11
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(2);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(2); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(3);  // fires
    debugListener.assertCurrThreadSuspendedCount(3);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
   
    if (printMessages) {
      System.out.println("third step");
    }
    // Step to line 12
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(3);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(3); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(4);  // fires
    debugListener.assertCurrThreadSuspendedCount(4);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    
    if (printMessages) {
      System.out.println("fourth step");
    }
    // Step to line 13
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(4);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(4); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(5);  // fires
    debugListener.assertCurrThreadSuspendedCount(5);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    
    if (printMessages) {
      System.out.println("fifth step");
    }
    // Step to line 14
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_OVER);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(5);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(5); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(6);  // fires
    debugListener.assertCurrThreadSuspendedCount(6);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    
    watches = _debugger.getWatches();
    assertEquals("watch name incorrect", "foo", watches.elementAt(0).getName());
    assertEquals("watch name incorrect", "innerFoo", watches.elementAt(1).getName());
    assertEquals("watch name incorrect", "innerInnerFoo", watches.elementAt(2).getName());
    assertEquals("watch name incorrect", "innerMethodFoo", watches.elementAt(3).getName());
    assertEquals("watch name incorrect", "asdf", watches.elementAt(4).getName());
    assertEquals("watch value incorrect", "7", watches.elementAt(0).getValue());
    assertEquals("watch value incorrect", "9", watches.elementAt(1).getValue());
    assertEquals("watch value incorrect", "11", watches.elementAt(2).getValue());
    assertEquals("watch value incorrect", "13", watches.elementAt(3).getValue());
    assertEquals("watch value incorrect", DebugWatchData.NO_VALUE, watches.elementAt(4).getValue());
    
    if (printMessages) {
      System.out.println("sixth step");
    }
    // Step into static method (line 15)
    synchronized(_notifierLock){
      _asyncStep(Debugger.STEP_INTO);
      _waitForNotifies(2);  // suspended, updated
      _notifierLock.wait();
    }
    debugListener.assertStepRequestedCount(6);  // fires (don't wait)
    debugListener.assertCurrThreadResumedCount(6); // fires (don't wait)
    debugListener.assertThreadLocationUpdatedCount(7);  // fires
    debugListener.assertCurrThreadSuspendedCount(7);  // fires
    debugListener.assertBreakpointReachedCount(1);
    debugListener.assertCurrThreadDiedCount(0);
    
    // Test watches in a static context.
    watches = _debugger.getWatches();
    assertEquals("watch name incorrect", "foo", watches.elementAt(0).getName());
    assertEquals("watch name incorrect", "innerFoo", watches.elementAt(1).getName());
    assertEquals("watch name incorrect", "innerInnerFoo", watches.elementAt(2).getName());
    assertEquals("watch name incorrect", "innerMethodFoo", watches.elementAt(3).getName());
    assertEquals("watch name incorrect", "asdf", watches.elementAt(4).getName());
    assertEquals("watch value incorrect", "7", watches.elementAt(0).getValue());
    assertEquals("watch value incorrect", DebugWatchData.NO_VALUE, watches.elementAt(1).getValue());
    assertEquals("watch value incorrect", DebugWatchData.NO_VALUE, watches.elementAt(2).getValue());
    assertEquals("watch value incorrect", DebugWatchData.NO_VALUE, watches.elementAt(3).getValue());
    assertEquals("watch value incorrect", DebugWatchData.NO_VALUE, watches.elementAt(4).getValue());
    
    // Close doc and make sure breakpoints are removed
    _model.closeFile(doc);
    debugListener.assertBreakpointRemovedCount(1);  //fires (no waiting)
    
    // Shutdown the debugger
    if (printMessages) {
      System.out.println("Shutting down...");
    }
    InterpretListener interpretListener = new InterpretListener() {
       public void interpreterChanged(boolean inProgress) {
         // Don't notify: happens in the same thread
        interpreterChangedCount++;
       }
     };
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(2);  // interactionEnded, shutdown
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);
    
    debugListener.assertDebuggerShutdownCount(1);  //fires
    if (printMessages) {
      System.out.println("Shut down.");
    }
    _debugger.removeListener(debugListener);
  }
  
  /**
   * Tests that watches can correctly see the values of local
   * variables, fields and fields of outer classes.
   */
  public void testStaticWatches()
    throws DebugException, BadLocationException, DocumentAdapterException,
    IOException, InterruptedException
  {
    if (printMessages) {
      System.out.println("----teststaticWatches----");
    }
    StepTestListener debugListener = new StepTestListener();
    
    // Compile the class
    File file = new File(_tempDir, "MonkeyStaticStuff.java");
    OpenDefinitionsDocument doc = doCompile(MONKEY_STATIC_STUFF, file);
    
    _debugger.addListener(debugListener);

    // Start debugger
    synchronized(_notifierLock) {
      _debugger.startup();
      _waitForNotifies(1);  // startup
      _notifierLock.wait();
    }
    debugListener.assertDebuggerStartedCount(1);
    
    int index = MONKEY_STATIC_STUFF.indexOf("System.out.println(MonkeyInner.MonkeyTwoDeep.twoDeepFoo);");
    _debugger.toggleBreakpoint(doc,
                               index,
                               14);
    debugListener.assertBreakpointSetCount(1);

    // Run the foo() method, hitting breakpoint
    synchronized(_notifierLock) {
      interpretIgnoreResult("MonkeyStaticStuff.MonkeyInner.MonkeyTwoDeep.MonkeyThreeDeep.threeDeepMethod();");
      _waitForNotifies(3);  // suspended, updated, breakpointReached
      _notifierLock.wait();
    }
    _debugger.addWatch("foo");
    _debugger.addWatch("innerFoo");
    _debugger.addWatch("twoDeepFoo");
    _debugger.addWatch("threeDeepFoo");
    _debugger.addWatch("asdf");
    
//    if (printMessages) {
//      System.out.println("first step");
//    }
//    // Step to line 14
//    synchronized(_notifierLock){
//      _asyncStep(Debugger.STEP_OVER);
//      _waitForNotifies(2);  // suspended, updated
//      _notifierLock.wait();
//    }
//    debugListener.assertStepRequestedCount(1);  // fires (don't wait)
//    debugListener.assertCurrThreadResumedCount(1); // fires (don't wait)
//    debugListener.assertThreadLocationUpdatedCount(2);  // fires
//    debugListener.assertCurrThreadSuspendedCount(2);  // fires
//    debugListener.assertBreakpointReachedCount(1);
//    debugListener.assertCurrThreadDiedCount(0);
    
    Vector<DebugWatchData> watches = _debugger.getWatches();
    assertEquals("watch name incorrect", "foo", watches.elementAt(0).getName());
    assertEquals("watch name incorrect", "innerFoo", watches.elementAt(1).getName());
    assertEquals("watch name incorrect", "twoDeepFoo", watches.elementAt(2).getName());
    assertEquals("watch name incorrect", "threeDeepFoo", watches.elementAt(3).getName());
    assertEquals("watch name incorrect", "asdf", watches.elementAt(4).getName());
    assertEquals("watch value incorrect", "6", watches.elementAt(0).getValue());
    assertEquals("watch value incorrect", "8", watches.elementAt(1).getValue());
    assertEquals("watch value incorrect", "13", watches.elementAt(2).getValue());
    assertEquals("watch value incorrect", "18", watches.elementAt(3).getValue());
    assertEquals("watch value incorrect", DebugWatchData.NO_VALUE, watches.elementAt(4).getValue());

    interpret("innerFoo = 0");
    watches = _debugger.getWatches();
    assertEquals("watch name incorrect", "innerFoo", watches.elementAt(1).getName());
    assertEquals("watch value incorrect", "0", watches.elementAt(1).getValue());

    interpret("innerFoo = 8");
    assertEquals("watch name incorrect", "innerFoo", watches.elementAt(1).getName());
    assertEquals("watch value incorrect", "8", watches.elementAt(1).getValue());
    
    // Close doc and make sure breakpoints are removed
    _model.closeFile(doc);
    debugListener.assertBreakpointRemovedCount(1);  //fires (no waiting)
    
    // Shutdown the debugger
    if (printMessages) {
      System.out.println("Shutting down...");
    }
    InterpretListener interpretListener = new InterpretListener() {
       public void interpreterChanged(boolean inProgress) {
         // Don't notify: happens in the same thread
        interpreterChangedCount++;
       }
     };
    _model.addListener(interpretListener);
    synchronized(_notifierLock) {
      _debugger.shutdown();
      _waitForNotifies(2);  // interactionEnded, shutdown
      _notifierLock.wait();
    }
    interpretListener.assertInteractionEndCount(1);
    _model.removeListener(interpretListener);
    
    debugListener.assertDebuggerShutdownCount(1);  //fires
    if (printMessages) {
      System.out.println("Shut down.");
    }
    _debugger.removeListener(debugListener);
  }
}
