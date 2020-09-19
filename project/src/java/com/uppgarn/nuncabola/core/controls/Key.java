/*
 * Key.java
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

package com.uppgarn.nuncabola.core.controls;

import com.uppgarn.codelibf.util.*;

//import org.lwjgl.input.*;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0;

public final class Key {
  private static final Map<String, Key> KEY_MAP = createKeyMap();
  
  private static Key[] getKeys() {
    return new Key[] {
      new Key("None", 0),
      
      new Key("0",             GLFW_KEY_0),
      new Key("1",             GLFW_KEY_1),
      new Key("2",             GLFW_KEY_2),
      new Key("3",             GLFW_KEY_3),
      new Key("4",             GLFW_KEY_4),
      new Key("5",             GLFW_KEY_5),
      new Key("6",             GLFW_KEY_6),
      new Key("7",             GLFW_KEY_7),
      new Key("8",             GLFW_KEY_8),
      new Key("9",             GLFW_KEY_9),
      new Key("Backspace",     GLFW_KEY_BACKSPACE),
      new Key("Caps Lock",     GLFW_KEY_CAPS_LOCK),
      new Key("Delete",        GLFW_KEY_DELETE),
      new Key("Down",          GLFW_KEY_DOWN),
      new Key("End",           GLFW_KEY_END),
      new Key("Escape",        GLFW_KEY_ESCAPE),
      new Key("F1",            GLFW_KEY_F1),
      new Key("F2",            GLFW_KEY_F2),
      new Key("F3",            GLFW_KEY_F3),
      new Key("F4",            GLFW_KEY_F4),
      new Key("F5",            GLFW_KEY_F5),
      new Key("F6",            GLFW_KEY_F6),
      new Key("F7",            GLFW_KEY_F7),
      new Key("F8",            GLFW_KEY_F8),
      new Key("F9",            GLFW_KEY_F9),
      new Key("F10",           GLFW_KEY_F10),
      new Key("F11",           GLFW_KEY_F11),
      new Key("F12",           GLFW_KEY_F12),
      new Key("Home",          GLFW_KEY_HOME),
      new Key("Insert",        GLFW_KEY_INSERT),
      new Key("Left",          GLFW_KEY_LEFT),
      new Key("Left Alt",      GLFW_KEY_LEFT_ALT),
      new Key("Left Control",  GLFW_KEY_LEFT_CONTROL),
      new Key("Left Meta",     GLFW_KEY_LEFT_SUPER),
      new Key("Left Shift",    GLFW_KEY_LEFT_SHIFT),
      new Key("Menu",          GLFW_KEY_MENU),
      new Key("Num Lock",      GLFW_KEY_NUM_LOCK),
      new Key("Numpad 0",      GLFW_KEY_KP_0),
      new Key("Numpad 1",      GLFW_KEY_KP_1),
      new Key("Numpad 2",      GLFW_KEY_KP_2),
      new Key("Numpad 3",      GLFW_KEY_KP_3),
      new Key("Numpad 4",      GLFW_KEY_KP_4),
      new Key("Numpad 5",      GLFW_KEY_KP_5),
      new Key("Numpad 6",      GLFW_KEY_KP_6),
      new Key("Numpad 7",      GLFW_KEY_KP_7),
      new Key("Numpad 8",      GLFW_KEY_KP_8),
      new Key("Numpad 9",      GLFW_KEY_KP_9),
      new Key("Numpad .",      GLFW_KEY_KP_DECIMAL),
      new Key("Numpad +",      GLFW_KEY_KP_ADD),
      new Key("Numpad -",      GLFW_KEY_KP_SUBTRACT),
      new Key("Numpad *",      GLFW_KEY_KP_MULTIPLY),
      new Key("Numpad /",      GLFW_KEY_KP_DIVIDE),
      new Key("Numpad Enter",  GLFW_KEY_KP_ENTER),
      new Key("Page Down",     GLFW_KEY_PAGE_DOWN),
      new Key("Page Up",       GLFW_KEY_PAGE_UP),
      new Key("Pause",         GLFW_KEY_PAUSE),
      new Key("Return",        GLFW_KEY_ENTER),
      new Key("Right",         GLFW_KEY_RIGHT),
      new Key("Right Alt",     GLFW_KEY_RIGHT_ALT),
      new Key("Right Control", GLFW_KEY_RIGHT_CONTROL),
      new Key("Right Meta",    GLFW_KEY_RIGHT_SUPER),
      new Key("Right Shift",   GLFW_KEY_RIGHT_SHIFT),
      new Key("Scroll Lock",   GLFW_KEY_SCROLL_LOCK),
      new Key("Space",         GLFW_KEY_SPACE),
      new Key("Sys Req",       GLFW_KEY_PRINT_SCREEN),
      new Key("Tab",           GLFW_KEY_TAB),
      new Key("Up",            GLFW_KEY_UP),
      
      new Key("Alt",     GLFW_KEY_LEFT_ALT,     GLFW_KEY_RIGHT_ALT),
      new Key("Control", GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL),
      new Key("Meta",    GLFW_KEY_LEFT_SUPER,   GLFW_KEY_RIGHT_SUPER),
      new Key("Shift",   GLFW_KEY_LEFT_SHIFT,   GLFW_KEY_RIGHT_SHIFT)
    };
  }
  
  private static Map<String, Key> createKeyMap() {
    Map<String, Key> map = new HashMap<>();
    
    for (Key key: getKeys()) {
      String name0 = key.name.toLowerCase(Locale.ENGLISH);
      String name1 = StringTool.remove(name0, ' ');
      
      map.put(name0, key);
      
      if (!name1.equals(name0)) {
        map.put(name1, key);
      }
    }
    
    return map;
  }
  
  public static Key get(String name) {
    // Search for predefined key.
    
    Key key = KEY_MAP.get(name.toLowerCase(Locale.ENGLISH));
    
    if (key != null) {
      return key;
    }
    
    // Interpret name as character.
    
    if (name.length() == 1) {
      char ch = Character.toLowerCase(name.charAt(0));
      
      return new Key(Character.toString(ch), 0, 0, ch);
    }
    
    return null;
  }
  
  private final String name;
  private final int    code0;
  private final int    code1;
  private final char   ch;
  
  private Key(String name, int code0) {
    this(name, code0, 0);
  }
  
  private Key(String name, int code0, int code1) {
    this(name, code0, code1, (char) 0);
  }
  
  private Key(String name, int code0, int code1, char ch) {
    this.name  = name;
    this.code0 = code0;
    this.code1 = code1;
    this.ch    = ch;
  }
  
  public boolean matches(int code, char ch) {
    return ((code0   != 0) && (code0   == code))
        || ((code1   != 0) && (code1   == code))
        || ((this.ch != 0) && (this.ch == Character.toLowerCase(ch)));
  }
  
  @Override
  public String toString() {
    return name;
  }
}
