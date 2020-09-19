/*
 * UI.java
 *
 * Copyright (c) 2003-2020 Nuncabola authors
 * See authors.txt for details.
 *
 * Nuncabola is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package com.uppgarn.nuncabola.ui;

import com.helixd2s.valera.VKt;
import com.helixd2s.valera.ValerABase;
import com.helixd2s.valera.ValerACore;
import com.uppgarn.nuncabola.core.audio.*;
import com.uppgarn.nuncabola.core.display.*;
import com.uppgarn.nuncabola.core.fps.*;
import com.uppgarn.nuncabola.core.graphics.*;
import com.uppgarn.nuncabola.core.gui.*;
import com.uppgarn.nuncabola.core.progress.*;
import com.uppgarn.nuncabola.core.renderers.*;
import com.uppgarn.nuncabola.functions.*;
import com.uppgarn.nuncabola.general.*;
import com.uppgarn.nuncabola.preferences.*;
import com.uppgarn.nuncabola.ui.hud.*;
import com.uppgarn.nuncabola.ui.resources.*;
import com.uppgarn.nuncabola.ui.screens.*;

import static com.uppgarn.nuncabola.functions.BaseFuncs.*;
import static org.lwjgl.glfw.GLFW.*;
//import static org.lwjgl.opengl.WGL.Functions.GetProcAddress;
import static org.lwjgl.glfw.GLFW.Functions.GetProcAddress;

import com.uppgarn.codelibf.io.*;

import org.bytedeco.javacpp.LongPointer;
import org.lwjgl.*;
//import org.lwjgl.input.*;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.opengl.*;
import org.lwjgl.vulkan.*;

import java.awt.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public final class UI {
  private static final PrintStream STD_OUT  =
    System.out;
  private static final PrintStream NULL_OUT =
    new PrintStream(NullOutputStream.getInstance());
  
  private static UIMode mode;

  private static boolean    windowActive;
  //private static Controller controller;
  private static float[]    axisValues;
  private static boolean    xAxisValid;
  private static boolean    yAxisValid;
  
  private static FPSCounter fpsCounter;
  private static HUD        hud;
  
  private static Screen  screen;
  private static float   screenTime;
  private static boolean timerEnabled;
  
  private static long startTime;
  private static long lastTime;
  
  private static UIException storedEx;
  public static long window = 0;
  public static VkInstance vInstance;
  public static VkPhysicalDevice vPhysicalDevice;
  public static VkDevice vDevice;
  public static ValerACore.Driver vDriver;
  public static LongPointer vInstanceHandle;
  public static LongPointer vDeviceHandle;
  public static LongPointer vPhysicalDeviceHandle;

  public static class KeyboardState {
    public static int     code = -1;
    public static boolean down = false;
    public static char    ch = ' ';
    public static char [] origChs;


    public static GLFWKeyCallback keyCallback;
  };

  public static class MouseState {
    public static int     x       = 0;
    public static int     y       = 0;
    public static int     dx      = 0;
    public static int     dy      = 0;
    public static int     button  = -1;
    public static boolean down    = false;
    public static int     wheel   = 0;
    public static boolean grabbed = false;

    public static GLFWMouseButtonCallback mouseCallback;
    public static GLFWCursorPosCallback posCallback;
  };


  //
  public static ValerABase.Framebuffer framebuffer;
  public static ValerABase.PipelineLayout pipelineLayout;
  public static ValerABase.TextureSet textureSet;
  public static ValerABase.SamplerSet samplerSet;
  public static ValerABase.Background background;
  public static ValerABase.MaterialSet materialSet;


  public static void initialize(UIMode mode) throws UIException {
    UI.mode = mode;
    
    try {
      initializeDisplay();
    } catch (IllegalStateException ex) {
      throw new UIException(ex);
    }
    try {
      initializeGfx();
    } catch (GfxException ex) {
      deinitializeDisplay();
      
      throw new UIException(ex);
    }
    
    Progress prg = new Progress(ImageResource.getImage("hourglass.dat", false));
    prg.show();
    
    initializeKeyboard();
    prg.show();
    initializeMouse();
    prg.show();
    initializeControllers();
    prg.show();
    initializeDataFuncs();
    prg.show();
    initializeRendererHome();
    prg.show();
    initializeGUIHome();
    prg.show();
    initializeAudio();
    prg.show();
    initializeFPSCounter();
    prg.show();
    initializeHUD();
    prg.show();
    initializeScreen();
    prg.show();
    initializeMenuSounds();
    prg.show();
    
    prg.fadeOut();
    prg.deinitialize();
    
    clearEvents();
    
    startTime = System.nanoTime();
    lastTime  = startTime;
    
    storedEx = null;
  }
  
  private static String[] getIconNames() {
    String os = System.getProperty("os.name");
    
    if (os.startsWith("Windows")) {
      return new String[] {"icon32.dat", "icon16.dat"};
    } else if (os.startsWith("Mac OS X") || os.startsWith("Darwin")) {
      return new String[] {"icon128.dat"};
    } else {
      return new String[] {"icon32.dat"};
    }
  }
  
  private static ByteBuffer[] getIcons() {
    String    [] names = getIconNames();
    ByteBuffer[] icons = new ByteBuffer[names.length];
    
    for (int idx = 0; idx < names.length; idx++) {
      icons[idx] = ImageResource.getImage(names[idx], true).getBuffer();
    }
    
    return icons;
  }

  private static void initializeDisplay() throws IllegalStateException {
    int     width       = getIntPref    (Pref.SCREEN_WIDTH);
    int     height      = getIntPref    (Pref.SCREEN_HEIGHT);
    boolean fullscreen  = getBooleanPref(Pref.FULLSCREEN);
    int     windowX     = getIntPref    (Pref.WINDOW_X);
    int     windowY     = getIntPref    (Pref.WINDOW_Y);
    boolean vSync       = getBooleanPref(Pref.V_SYNC);
    boolean reflection  = getBooleanPref(Pref.REFLECTION);
    int     multisample = getIntPref    (Pref.MULTISAMPLE);

    DisplayMode displayMode = DisplayTool.getMode(width, height);

    width      = displayMode.getWidth ();
    height     = displayMode.getHeight();
    fullscreen = fullscreen && false; //fullscreen && displayMode.isFullscreenCapable();

    /*
    Display.setDisplayMode (displayMode);
    Display.setFullscreen  (fullscreen);
    Display.setLocation    (windowX, windowY);
    Display.setVSyncEnabled(vSync);
    Display.setTitle       (ProgramConstants.TITLE);
    Display.setIcon        (getIcons());

    while (true) {
      PixelFormat format = new PixelFormat()
        .withBitsPerPixel(15)
        .withDepthBits   (16)
        .withStencilBits (reflection ? 1 : 0)
        .withSamples     (multisample);

      try {
        Display.create(format);

        break;
      } catch (IllegalStateException ex) {
        // If display creation failed, try again
        // with gradually lowered requirements.
        
        if (multisample > 0) {
          multisample /= 2;
        } else if (reflection) {
          reflection   = false;
        } else {
          // Give up.
          
          throw ex;
        }
      }
    }*/

    window = 0;
    if (fullscreen) {} else {
      // Configure GLFW
      glfwDefaultWindowHints();
      glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
      glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
      glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
      //glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, 1);
      glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
      glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
      ///glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);

      // Create the window
      long monitor = glfwGetPrimaryMonitor();
      window = glfwCreateWindow(width, height, "Hello World!", 0, 0);
      if (window == 0) {
        throw new RuntimeException("Failed to create the GLFW window");
      }

      //
      glfwMakeContextCurrent(window);
      GL.createCapabilities();
      VKt.initializeGL(GetProcAddress);

      // Java 8
      glfwSetMouseButtonCallback(window, MouseState.mouseCallback = GLFWMouseButtonCallback.create((window, button, action, mods) -> {
        UI.MouseState.button = button;
        if (action == GLFW_PRESS) { UI.MouseState.down = true; };
        if (action == GLFW_RELEASE) { UI.MouseState.down = false; };

        if (screen == null) { return; };
        UI.handleMouseEvent();
      }));

      // Java 8
      glfwSetCursorPosCallback(window, MouseState.posCallback = GLFWCursorPosCallback.create((window, xpos, ypos) -> {
        UI.MouseState.dx = (int)(xpos - (double)UI.MouseState.x);
        UI.MouseState.dy = (int)(ypos - (double)UI.MouseState.y);
        UI.MouseState.x = (int)xpos;
        UI.MouseState.y = (int)ypos;
        UI.MouseState.button = -1;

        if (screen == null) { return; };
        UI.handleMouseEvent();
      }));

      // With Java 8 you could also use lambda expressions for setting a callback
      glfwSetKeyCallback(window, KeyboardState.keyCallback = GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
        UI.KeyboardState.code = key;
        if (action == GLFW_PRESS) {
          UI.KeyboardState.down = true;
        };
        if (action == GLFW_RELEASE) {
          UI.KeyboardState.down = false;
        };

        String charcode = glfwGetKeyName(key, scancode);
        if (charcode != null) {
          UI.KeyboardState.ch = charcode.charAt(0);
        };


        if (screen == null) { return; };
        UI.handleKeyEvent();
      }));
    }

    //
    vDriver = new ValerACore.Driver();
    vInstanceHandle = vDriver.createInstance();
    vInstance = new VkInstance(vInstanceHandle.get(), VkInstanceCreateInfo.create(vDriver.getInstanceCreateInfoAddress()));
    vPhysicalDeviceHandle = vDriver.getPhysicalDevice();
    vPhysicalDevice = new VkPhysicalDevice(vPhysicalDeviceHandle.get(), vInstance);
    vDeviceHandle = vDriver.createDevice();
    vDevice = new VkDevice(vDeviceHandle.get(), vPhysicalDevice, VkDeviceCreateInfo.create(vDriver.getDeviceCreateInfoAddress()));

    //
    setPref(Pref.SCREEN_WIDTH,  width);
    setPref(Pref.SCREEN_HEIGHT, height);
    setPref(Pref.FULLSCREEN,    fullscreen);
    setPref(Pref.REFLECTION,    reflection);
    setPref(Pref.MULTISAMPLE,   multisample);

    //
    ValerACore.DataSetCreateInfo info = new ValerACore.DataSetCreateInfo();
    info.count().address();

    //
    framebuffer = new ValerABase.Framebuffer(vDriver.uniPtr());
    pipelineLayout = new ValerABase.PipelineLayout(vDriver.uniPtr());
    textureSet = new ValerABase.TextureSet(vDriver.uniPtr());
    samplerSet = new ValerABase.SamplerSet(vDriver.uniPtr());
    background = new ValerABase.Background(vDriver.uniPtr());
    materialSet = new ValerABase.MaterialSet(vDriver.uniPtr(), info);

    //
    framebuffer.createFramebuffer(width, height);


    windowActive = false;
  }
  
  private static void initializeGfx() throws GfxException {
    Gfx.initialize(
      getIntPref    (Pref.SCREEN_WIDTH),
      getIntPref    (Pref.SCREEN_HEIGHT),
      getBooleanPref(Pref.REFLECTION),
      getIntPref    (Pref.MULTISAMPLE),
      getBooleanPref(Pref.MIPMAP),
      getIntPref    (Pref.ANISO),
      getIntPref    (Pref.TEXTURES));
  }
  
  private static void initializeKeyboard() {
    /*
    try {
      Keyboard.create();
      Keyboard.enableRepeatEvents(false);
    } catch (LWJGLException ex) {
      // No keyboard available.
    }
    */

    KeyboardState.origChs = new char[512];
  }
  
  private static void initializeMouse() {
    /*
    try {
      Mouse.create();
    } catch (LWJGLException ex) {
      // No mouse available.
    }
    */
  }

  private static void initializeControllers() {
    /*controller = null;

    if (getBooleanPref(Pref.CONTROLLER)) {
      try {
        System.setOut(NULL_OUT);
        try {
          Controllers.create(); // JInput may print warnings during this call
        } finally {
          System.setOut(STD_OUT);
        }
        
        int idx = getIntPref(Pref.CONTROLLER_INDEX);
        
        if (idx < Controllers.getControllerCount()) {
          controller = Controllers.getController(idx);
          axisValues = new float[controller.getAxisCount()];
          
          // Try to configure axes automatically.
          
          int xAxis = getIntPref(Pref.CONTROLLER_AXIS_X);
          int yAxis = getIntPref(Pref.CONTROLLER_AXIS_Y);
          int zAxis = getIntPref(Pref.CONTROLLER_AXIS_Z);
          
          int foundXAxis  = -1;
          int foundYAxis  = -1;
          int foundZAxis  = -1;
          int foundRXAxis = -1;
          
          for (int axis = controller.getAxisCount() - 1; axis >= 0; axis--) {
            String name = controller
              .getAxisName(axis)
              .toLowerCase(Locale.ENGLISH)
              .replace('-', ' ');
            
            switch (name) {
              case "x":
              case "x axis":
              case "left x": {
                foundXAxis = axis;
                
                break;
              }
              case "y":
              case "y axis":
              case "left y": {
                foundYAxis = axis;
                
                break;
              }
              case "z":
              case "z axis": {
                foundZAxis = axis;
                
                break;
              }
              case "rx":
              case "rx axis":
              case "x rotation":
              case "right x": {
                foundRXAxis = axis;
                
                break;
              }
            }
          }
          
          if (xAxis == -1) {
            xAxis = foundXAxis;
          }
          if (yAxis == -1) {
            yAxis = foundYAxis;
          }
          if (zAxis == -1) {
            zAxis = (foundRXAxis != -1) ? foundRXAxis : foundZAxis;
          }
          
          if ((xAxis == -1) || (yAxis == -1)) {
            xAxis = 0;
            yAxis = 1;
          }
          
          setPref(Pref.CONTROLLER_AXIS_X, xAxis);
          setPref(Pref.CONTROLLER_AXIS_Y, yAxis);
          setPref(Pref.CONTROLLER_AXIS_Z, zAxis);
        }
      } catch (IllegalStateException ex) {
        // No controllers available.
      }
    }
    
    xAxisValid = true;
    yAxisValid = true;*/
  }

  private static void initializeDataFuncs() {
    DataFuncs.initialize();
  }
  
  private static void initializeRendererHome() {
    RendererHome.initialize(
      DataFuncs.getDataFolder(),
      getBooleanPref(Pref.BACKGROUND),
      getBooleanPref(Pref.SHADOW),
      getIntPref    (Pref.VIEW_FOV),
      getStringPref (Pref.BALL_PATH));
  }
  
  private static void initializeGUIHome() {
    GUIHome.initialize(DataFuncs.getDataFolder(), getStringPref(Pref.THEME));
  }

  private static void initializeAudio() {
    Audio.initialize(DataFuncs.getDataFolder(), getIntPref(Pref.AUDIO_BUFFER));
    Audio.setSoundVolume(getIntPref(Pref.VOLUME_SOUND));
    Audio.setMusicVolume(getIntPref(Pref.VOLUME_MUSIC));
  }
  
  private static void initializeFPSCounter() {
    fpsCounter = new FPSCounter();
  }
  
  private static void initializeHUD() {
    hud = new HUD(fpsCounter);
  }
  
  private static void initializeScreen() {
    screen       = null;
    screenTime   = 0.0f;
    timerEnabled = false;
    
    switch (mode.getType()) {
      case STANDARD: {
        // Standard mode.
        
        gotoScreen(TitleScreen.INSTANCE);
        
        break;
      }
      case REPLAY: {
        // Replay mode.
        
        UIMode.Replay myMode = (UIMode.Replay) mode;
        
        try {
          ReplayFuncs.initialize(myMode.getFile());
          
          if (GameFuncs.getGame().levelCompatible) {
            gotoScreen(ReplayIntroScreen  .INSTANCE);
          } else {
            gotoScreen(ReplayWarningScreen.INSTANCE);
          }
        } catch (FuncsException ex) {
          gotoScreen(TitleScreen.INSTANCE);
        }
        
        break;
      }
    }
  }
  
  private static void initializeMenuSounds() {
    // Hack: Preload selected sounds by playing them with volume set to zero.
    
    Audio.playSound("snd/menu.ogg",   0.0f);
    Audio.playSound("snd/select.ogg", 0.0f);
  }

  private static void clearEvents() {
    /*
    if (Keyboard.isCreated()) {
      while (Keyboard.next()) {
      }
    }
    if (Mouse.isCreated()) {
      while (Mouse.next()) {
      }
    }
    if (Controllers.isCreated()) {
      while (Controllers.next()) {
      }
    }
    
    if ((screen != null) && Mouse.isCreated()) {
      screen.mouseMove(Mouse.getX(), Mouse.getY(), 0, 0);
    }
    */

    int     width       = getIntPref    (Pref.SCREEN_WIDTH);
    int     height      = getIntPref    (Pref.SCREEN_HEIGHT);

    if (screen != null) {
      double[] xpos = new double[1], ypos = new double[1];
      glfwGetCursorPos(window, xpos, ypos);
      //glfwSetCursorPos(window, xpos[0], ypos[0]);
      screen.mouseMove((int)xpos[0], height-(int)ypos[0], 0, 0);
    }
  }
  
  public static UIMode getMode() {
    return mode;
  }
  
  public static HUD getHUD() {
    return hud;
  }

  // TODO: Grab Mouse from GLFW-3
  public static void setMouseGrabbed(boolean grabbed) {
    /*if (Mouse.isCreated() && (Mouse.isGrabbed() != grabbed)) {
      Mouse.setGrabbed(grabbed);
    }*/
  }

  public static float getScreenTime() {
    return screenTime;
  }
  
  public static void gotoScreen(Screen newScreen) {
    gotoScreen(newScreen, false);
  }
  
  public static void gotoScreen(Screen newScreen, boolean seamless) {
    if (screen != null) {
      screen.leave(newScreen);
    }
    
    Screen oldScreen = screen;
    
    screen       = newScreen;
    screenTime   = 0.0f;
    timerEnabled = timerEnabled && seamless;
    
    if (screen != null) {
      screen.enter(oldScreen);
    }
  }
  
  private static void checkForWindowCloseRequest() {
    //if (Display.isCloseRequested()) {
    //  gotoScreen(null);
    //}
    if (glfwWindowShouldClose(window) ) {
      gotoScreen(null);
    }
  }
  
  private static void checkForWindowDeactivation() {
    //if (!Display.isActive()) {
    if (glfwGetWindowAttrib(window, GLFW_FOCUSED) == 0) {
      if (windowActive) {
        windowActive = false;

        screen.windowDeactivated();
      }
    } else {
      windowActive = true;
    }
  }

  public static void handleKeyEvent() {
    int     code = KeyboardState.code;//Keyboard.getEventKey();
    char    ch   = KeyboardState.ch;//Keyboard.getEventCharacter();
    boolean down = KeyboardState.down;//Keyboard.getEventKeyState();

    if (down) {
      // Key down.
      
      KeyboardState.origChs[code] = ch;
      
      switch (code) {
        case GLFW_KEY_F6: {
          if (getBooleanPref(Pref.CHEAT)) {
            Gfx.toggleWire();
          }
          
          break;
        }
        case GLFW_KEY_F9: {
          setPref(Pref.FPS, !getBooleanPref(Pref.FPS));
          
          break;
        }
        case GLFW_KEY_F12: {
          ScreenshotFuncs.takeScreenshot();
          
          break;
        }
        case GLFW_KEY_ESCAPE: {
          screen.exitRequested();
          
          break;
        }
        
        default: {
          screen.keyDown(code, ch);
          
          break;
        }
      }
      
      if (screen == null) {
        return;
      }
      
      if (ch >= ' ') {
        screen.textEntered(ch);
      }
    } else {
      // Key up.
      
      screen.keyUp(code, ch, KeyboardState.origChs[code]);
      
      if (screen == null) {
        return;
      }
      
      // Support for Alt+NumPad input method (Windows).
      
      if (((code == GLFW_KEY_LEFT_ALT) || (code == GLFW_KEY_RIGHT_ALT))
          && (ch >= ' ')) {
        screen.textEntered(ch);
      }
    }
  }

  public static void handleMouseEvent() {
    int     x       = MouseState.x;
    int     y       = MouseState.y;
    int     dx      = MouseState.dx;
    int     dy      = MouseState.dy;
    int     button  = MouseState.button;
    boolean down    = MouseState.down;
    int     wheel   = MouseState.wheel;
    boolean grabbed = MouseState.grabbed;

    int     width       = getIntPref    (Pref.SCREEN_WIDTH);
    int     height      = getIntPref    (Pref.SCREEN_HEIGHT);

    if (button != -1) {
      // Button.
      
      if (down) {
        screen.mouseDown(button);
      } else {
        screen.mouseUp  (button);
      }
    } else if (wheel != 0) {
      // Wheel.
      
      screen.mouseWheel((wheel < 0) ? -1 : +1);
    } else {
      // Movement.
      
      if (grabbed) {
        int myDY = getBooleanPref(Pref.MOUSE_INVERT) ? +dy : -dy;
        
        screen.mouseMove(0, 0, dx, myDY);
      } else {
        screen.mouseMove(x, height-y, 0, 0);
      }
    }
  }

  private static void handleControllerEvent() {
    /*
    if (Controllers.getEventSource() != controller) {
      return;
    }
    
    int     axis   = -1;
    float   value  = 0.0f;
    int     button = -1;
    boolean down   = false;
    
    if (Controllers.isEventPovX()) {
      // D-pad x-axis.
      
      axis   = getIntPref(Pref.CONTROLLER_AXIS_X);
      value  = controller.getPovX();
    } else if (Controllers.isEventPovY()) {
      // D-pad y-axis.
      
      axis   = getIntPref(Pref.CONTROLLER_AXIS_Y);
      value  = controller.getPovY();
    } else if (Controllers.isEventAxis()) {
      axis   = Controllers.getEventControlIndex();
      value  = controller.getAxisValue(axis);
      
      if (axisValues[axis] != value) {
        axisValues[axis] = value;
      } else {
        return;
      }
    } else if (Controllers.isEventButton()) {
      button = Controllers.getEventControlIndex();
      down   = controller.isButtonPressed(button);
    }
    
    if (axis != -1) {
      // Movement.
      
      boolean invert;
      boolean recentered;
      
      boolean significant = Math.abs(value) > 0.5f;
      
      if (axis == getIntPref(Pref.CONTROLLER_AXIS_X)) {
        // X-axis.
        
        invert     = getBooleanPref(Pref.CONTROLLER_AXIS_X_INVERT);
        recentered = xAxisValid;
        
        xAxisValid = !significant;
      } else if (axis == getIntPref(Pref.CONTROLLER_AXIS_Y)) {
        // Y-axis.
        
        invert     = getBooleanPref(Pref.CONTROLLER_AXIS_Y_INVERT);
        recentered = yAxisValid;
        
        yAxisValid = !significant;
      } else if (axis == getIntPref(Pref.CONTROLLER_AXIS_Z)) {
        // Z-axis.
        
        invert     = getBooleanPref(Pref.CONTROLLER_AXIS_Z_INVERT);
        recentered = false;
      } else {
        invert     = false;
        recentered = false;
      }
      
      float myValue = invert ? -value : +value;
      
      screen.controllerMove(axis, myValue, recentered);
    } else if (button != -1) {
      // Button.
      
      if (down) {
        if (button == getIntPref(Pref.CONTROLLER_BUTTON_EXIT)) {
          screen.exitRequested();
        } else {
          screen.controllerDown(button);
        }
      } else {
        screen.controllerUp(button);
      }
    }
    */
  }
  
  private static void timer() {
    // Determine delta time and clamp it to 250ms.
    
    long  time = System.nanoTime();
    float dt   = Math.min((float) ((time - lastTime) / 1000000000.0), 0.25f);
    
    lastTime = time;
    
    if (dt <= 0.0f) {
      return;
    }

    // Step subsystems.
    RendererHome.step(dt);
    
    // Pass the time to the screen if enabled.
    
    if (!timerEnabled) {
      timerEnabled = true;
    } else {
      screenTime += dt;
      
      screen.timer(dt);
    }
  }
  
  private static void paint() {
    float t = (float) ((System.nanoTime() - startTime) / 1000000000.0);
    
    Gfx.clear();
    
    screen.paint(t);
  }
  
  private static void updateFPSCounter(boolean painted) {
    if (fpsCounter.update(painted)) {
      // Output statistics if configured.
      
      if (getBooleanPref(Pref.STATS)) {
        int   fps = fpsCounter.getFPS();
        float ms  = fpsCounter.getMS();
        
        System.out.printf("%4d %8.4f%n", fps, ms);
      }
    }
  }
  
  private static void loopIteration() {
    // Update display.
    
    System.setOut(NULL_OUT);

    try {
      //Display.update(); // JInput may print warnings during this call
      if (UI.window != 0) { glfwSwapBuffers(UI.window); };
    } finally {
      System.setOut(STD_OUT);
    }
    
    // Window close request.
    checkForWindowCloseRequest();
    
    if (screen == null) {
      return;
    }
    
    // Window deactivation.
    
    checkForWindowDeactivation();
    
    if (screen == null) {
      return;
    }

    //UI.handleKeyEvent();
    //UI.handleMouseEvent();

    /*
    // Key events.

    if (Keyboard.isCreated()) {
      while (Keyboard.next()) {
        handleKeyEvent();
        
        if (screen == null) {
          return;
        }
      }
    }
    
    // Mouse events.
    
    if (Mouse.isCreated()) {
      while (Mouse.next()) {
        handleMouseEvent();
        
        if (screen == null) {
          return;
        }
      }
    }
    
    // Controller events.
    
    if (Controllers.isCreated()) {
      while (Controllers.next()) {
        handleControllerEvent();
        
        if (screen == null) {
          return;
        }
      }
    }*/
    
    // Timer.
    
    timer();
    
    if (screen == null) {
      return;
    }

    // Paint.
    if (glfwGetWindowAttrib(window, GLFW_FOCUSED) != 0) { // TODO:
      paint();
      
      updateFPSCounter(true);
    } else {
      // Skip painting when window is minimized. Instead,
      // temporarily cease execution to reduce CPU usage.
      
      try {
        Thread.sleep(1);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      
      updateFPSCounter(false);
    }
  }
  
  public static void loop(ErrorLogger errorLogger) throws UIException {
    while (screen != null) {
      glfwPollEvents();

      try {
        loopIteration();
      } catch (Throwable t) {
        // Log error.
        
        errorLogger.log(t);
        
        t.printStackTrace();
      }
    }
    
    if (storedEx != null) {
      throw storedEx;
    }
  }
  
  public static void rebuildRenderers() {
    deinitializeGameFuncs();
    deinitializeRendererHome();
    
    initializeRendererHome();
  }
  
  public static void rebuild() throws UIException {
    //if (Display.isCreated()) {
    if (window != 0) { // TODO:
      deinitializeHUD();
      deinitializeGameFuncs();
      deinitializeGUIHome();
      deinitializeRendererHome();
      deinitializeControllers();
      deinitializeMouse();
      deinitializeKeyboard();
      deinitializeGfx();
      deinitializeDisplay();
    }

    try {
      initializeDisplay();
    } catch (IllegalStateException ex) {
      storedEx = new UIException(ex);
      
      throw storedEx;
    }
    try {
      initializeGfx();
    } catch (GfxException ex) {
      deinitializeDisplay();
      
      storedEx = new UIException(ex);
      
      throw storedEx;
    }
    
    initializeKeyboard();
    initializeMouse();
    initializeControllers();
    initializeRendererHome();
    initializeGUIHome();
    initializeHUD();
    
    storedEx = null;
  }
  
  private static void deinitializeDisplay() {
    /* TODO:
    if (!Display.isFullscreen() && Display.isVisible()) {
      setPref(Pref.WINDOW_X, Math.max(Display.getX(), 0));
      setPref(Pref.WINDOW_Y, Math.max(Display.getY(), 0));
    }
    */
    
    //Display.destroy();
  }
  
  private static void deinitializeGfx() {
    Gfx.deinitialize();
  }
  
  private static void deinitializeKeyboard() {
    //Keyboard.destroy();
    
    KeyboardState.origChs = null;
  }
  
  private static void deinitializeMouse() {
    //Mouse.destroy();
  }
  
  private static void deinitializeControllers() {
    //Controllers.destroy();
    
    //controller = null;
    axisValues = null;
  }
  
  private static void deinitializeDataFuncs() {
    DataFuncs.deinitialize();
  }
  
  private static void deinitializeRendererHome() {
    RendererHome.deinitialize();
  }
  
  private static void deinitializeGUIHome() {
    GUIHome.deinitialize();
  }
  
  private static void deinitializeAudio() {
    Audio.deinitialize();
  }
  
  private static void deinitializeGameFuncs() {
    GameFuncs.deinitialize();
  }
  
  private static void deinitializeFPSCounter() {
    fpsCounter = null;
  }
  
  private static void deinitializeHUD() {
    hud.deinitialize();
    
    hud = null;
  }

  public static void deinitialize() {
    //if (Display.isCreated()) {
    if (window != 0) {
      deinitializeHUD();
      deinitializeFPSCounter();
      deinitializeGameFuncs();
      deinitializeAudio();
      deinitializeGUIHome();
      deinitializeRendererHome();
      deinitializeDataFuncs();
      deinitializeControllers();
      deinitializeMouse();
      deinitializeKeyboard();
      deinitializeGfx();
      deinitializeDisplay();
    } else {
      deinitializeFPSCounter();
      deinitializeAudio();
      deinitializeDataFuncs();
    }
    glfwTerminate();
    storedEx = null;
  }
  
  private UI() {
  }
}
