/*
 * ALBufferedSource.java
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

package com.uppgarn.nuncabola.core.audio;

import static com.uppgarn.nuncabola.core.audio.AudioConstants.*;

import org.lwjgl.openal.*;

import static org.lwjgl.openal.AL10.*;

final class ALBufferedSource {
  private Integer source;
  private Integer buffer;
  
  private boolean errorOccurred;
  
  public ALBufferedSource(AudioData data) {
    source = createSource();
    buffer = createBuffer();
    
    errorOccurred = (source == null) || (buffer == null);
    
    initializeBuffer(data);
  }
  
  private Integer createSource() {
    try {
      return alGenSources();
    } catch (IllegalStateException ex) {
      return null;
    }
  }
  
  private Integer createBuffer() {
    try {
      return alGenBuffers();
    } catch (IllegalStateException ex) {
      return null;
    }
  }
  
  private void initializeBuffer(AudioData data) {
    if (errorOccurred) {
      return;
    }
    
    try {
      int format = data.isMono() ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
      
      alBufferData(buffer, format, data.getBuffer(), SAMPLE_RATE);
      alSourceQueueBuffers(source, buffer);
    } catch (IllegalStateException ex) {
      errorOccurred = true;
    }
  }
  
  public void setVolume(float volume) {
    if (errorOccurred) {
      return;
    }
    
    try {
      alSourcef(source, AL_GAIN, volume);
    } catch (IllegalStateException ex) {
      errorOccurred = true;
    }
  }
  
  public boolean isStopped() {
    if (errorOccurred) {
      return true;
    }
    
    try {
      return alGetSourcei(source, AL_SOURCE_STATE) == AL_STOPPED;
    } catch (IllegalStateException ex) {
      errorOccurred = true;
      
      return true;
    }
  }
  
  public void play() {
    if (errorOccurred) {
      return;
    }
    
    try {
      alSourcePlay(source);
    } catch (IllegalStateException ex) {
      errorOccurred = true;
    }
  }
  
  public void pause() {
    if (errorOccurred) {
      return;
    }
    
    try {
      alSourcePause(source);
    } catch (IllegalStateException ex) {
      errorOccurred = true;
    }
  }
  
  public void rewind() {
    if (errorOccurred) {
      return;
    }
    
    try {
      alSourcePause (source); // workaround
      alSourceRewind(source);
    } catch (IllegalStateException ex) {
      errorOccurred = true;
    }
  }
  
  public void deinitialize() {
    if (source != null) {
      try {
        alDeleteSources(source);
      } catch (IllegalStateException ex) {
      }
    }
    if (buffer != null) {
      try {
        alDeleteBuffers(buffer);
      } catch (IllegalStateException ex) {
      }
    }
  }
}
