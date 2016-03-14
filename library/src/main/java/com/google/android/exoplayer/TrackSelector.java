/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer;

import android.util.Pair;

/**
 * Selects tracks to be consumed by available {@link TrackRenderer}s.
 */
public abstract class TrackSelector {

  /**
   * Notified when previous selections by a {@link TrackSelector} are no longer valid.
   */
  /* package */ interface InvalidationListener {

    /**
     * Invoked by a {@link TrackSelector} when previous selections are no longer valid.
     */
    void onTrackSelectionsInvalidated();

  }

  private InvalidationListener listener;

  /* package */ void init(InvalidationListener listener) {
    this.listener = listener;
  }

  /**
   * Invalidates all previously generated track selections.
   */
  protected final void invalidate() {
    if (listener != null) {
      listener.onTrackSelectionsInvalidated();
    }
  }

  /**
   * Generates a {@link TrackSelection} for each renderer.
   * <P>
   * The selections are returned in a {@link TrackSelectionArray}, together with an opaque object
   * that the selector wishes to receive in an invocation of {@link #onSelectionActivated(Object)}
   * should the selection be activated.
   *
   * @param renderers The renderers.
   * @param trackGroups The available track groups.
   * @return A {@link TrackSelectionArray} containing a {@link TrackSelection} for each renderer,
   *     together with an opaque object that will be passed to {@link #onSelectionActivated(Object)}
   *     if the selection is activated.
   * @throws ExoPlaybackException If an error occurs selecting tracks.
   */
  protected abstract Pair<TrackSelectionArray, Object> selectTracks(TrackRenderer[] renderers,
      TrackGroupArray trackGroups) throws ExoPlaybackException;

  /**
   * Invoked when a selection previously generated by
   * {@link #selectTracks(TrackRenderer[], TrackGroupArray)} is activated.
   *
   * @param selectionInfo The opaque object associated with the selection.
   */
  protected abstract void onSelectionActivated(Object selectionInfo);

}
