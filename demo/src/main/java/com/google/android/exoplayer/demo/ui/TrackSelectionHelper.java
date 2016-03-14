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
package com.google.android.exoplayer.demo.ui;

import com.google.android.exoplayer.DefaultTrackSelector;
import com.google.android.exoplayer.DefaultTrackSelector.TrackInfo;
import com.google.android.exoplayer.Format;
import com.google.android.exoplayer.TrackGroup;
import com.google.android.exoplayer.TrackGroupArray;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.TrackSelection;
import com.google.android.exoplayer.demo.R;
import com.google.android.exoplayer.util.MimeTypes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import java.util.Arrays;
import java.util.Locale;

/**
 * Helper class for displaying track selection dialogs.
 */
public class TrackSelectionHelper implements View.OnClickListener, DialogInterface.OnClickListener {

  private final DefaultTrackSelector selector;

  private CheckedTextView disableView;
  private CheckedTextView defaultView;
  private CheckedTextView[][] trackViews;

  private TrackInfo trackInfo;
  private int rendererIndex;
  private TrackGroupArray trackGroups;
  private boolean[] trackGroupsAdaptive;

  private boolean isDisabled;
  private TrackSelection override;

  /**
   * @param selector The track selector.
   */
  public TrackSelectionHelper(DefaultTrackSelector selector) {
    this.selector = selector;
  }

  /**
   * Shows the selection dialog for a given renderer.
   *
   * @param activity The parent activity.
   * @param titleId The dialog's title.
   * @param trackInfo The current track information.
   * @param rendererIndex The index of the renderer.
   */
  public void showSelectionDialog(Activity activity, int titleId, TrackInfo trackInfo,
      int rendererIndex) {
    this.trackInfo = trackInfo;
    this.rendererIndex = rendererIndex;

    trackGroups = trackInfo.getTrackGroups(rendererIndex);
    trackGroupsAdaptive = new boolean[trackGroups.length];
    for (int i = 0; i < trackGroups.length; i++) {
      trackGroupsAdaptive[i] = trackInfo.getAdaptiveSupport(rendererIndex, i, false)
          != TrackRenderer.ADAPTIVE_NOT_SUPPORTED;
    }
    isDisabled = selector.getRendererDisabled(rendererIndex);
    if (selector.hasSelectionOverride(rendererIndex, trackGroups)) {
      override = trackInfo.getTrackSelection(rendererIndex);
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    builder.setTitle(titleId)
        .setView(buildView(LayoutInflater.from(builder.getContext())))
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel, null)
        .create()
        .show();
  }

  @SuppressLint("InflateParams")
  private View buildView(LayoutInflater inflater) {
    ViewGroup root = (ViewGroup) inflater.inflate(R.layout.track_selection_dialog, null);

    // View for disabling the renderer.
    disableView = (CheckedTextView) inflater.inflate(
        android.R.layout.simple_list_item_single_choice, root, false);
    disableView.setText(R.string.selection_disabled);
    disableView.setOnClickListener(this);
    root.addView(disableView);

    // View for clearing the override to allow the selector to use its default selection logic.
    defaultView = (CheckedTextView) inflater.inflate(
        android.R.layout.simple_list_item_single_choice, root, false);
    defaultView.setText(R.string.selection_default);
    defaultView.setOnClickListener(this);
    root.addView(inflater.inflate(R.layout.list_divider, root, false));
    root.addView(defaultView);

    // Per-track views.
    boolean haveSupportedTracks = false;
    trackViews = new CheckedTextView[trackGroups.length][];
    for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
      root.addView(inflater.inflate(R.layout.list_divider, root, false));
      TrackGroup group = trackGroups.get(groupIndex);
      trackViews[groupIndex] = new CheckedTextView[group.length];
      for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
        int trackViewLayoutId = group.length < 2 || !trackGroupsAdaptive[groupIndex]
            ? android.R.layout.simple_list_item_single_choice
            : android.R.layout.simple_list_item_multiple_choice;
        CheckedTextView trackView = (CheckedTextView) inflater.inflate(
            trackViewLayoutId, root, false);
        trackView.setText(buildTrackName(group.getFormat(trackIndex)));
        if (trackInfo.getTrackFormatSupport(rendererIndex, groupIndex, trackIndex)
            == TrackRenderer.FORMAT_HANDLED) {
          haveSupportedTracks = true;
          trackView.setTag(Pair.create(groupIndex, trackIndex));
          trackView.setOnClickListener(this);
        } else {
          trackView.setEnabled(false);
        }
        trackViews[groupIndex][trackIndex] = trackView;
        root.addView(trackView);
      }
    }

    if (!haveSupportedTracks) {
      // Indicate that the default selection will be nothing.
      defaultView.setText(R.string.selection_default_none);
    }

    updateViews();
    return root;
  }

  private void updateViews() {
    disableView.setChecked(isDisabled);
    defaultView.setChecked(!isDisabled && override == null);
    for (int i = 0; i < trackViews.length; i++) {
      for (int j = 0; j < trackViews[i].length; j++) {
        trackViews[i][j].setChecked(
            override != null && override.group == i && override.containsTrack(j));
      }
    }
  }

  // DialogInterface.OnClickListener

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (isDisabled) {
      selector.setRendererDisabled(rendererIndex, true);
      return;
    }
    selector.setRendererDisabled(rendererIndex, false);
    if (override != null) {
      selector.setSelectionOverride(rendererIndex, trackGroups, override);
    } else {
      selector.clearSelectionOverrides(rendererIndex);
    }
  }

  // View.OnClickListener

  @Override
  public void onClick(View view) {
    if (view == disableView) {
      isDisabled = true;
      override = null;
    } else if (view == defaultView) {
      isDisabled = false;
      override = null;
    } else {
      isDisabled = false;
      @SuppressWarnings("unchecked")
      Pair<Integer, Integer> tag = (Pair<Integer, Integer>) view.getTag();
      int groupIndex = tag.first;
      int trackIndex = tag.second;
      if (!trackGroupsAdaptive[groupIndex] || override == null) {
        override = new TrackSelection(groupIndex, trackIndex);
      } else {
        // The group being modified is adaptive and we already have a non-null override.
        boolean isEnabled = ((CheckedTextView) view).isChecked();
        if (isEnabled) {
          // Remove the track from the override.
          if (override.length == 1) {
            // The last track is being removed, so the override becomes empty.
            override = null;
          } else {
            int[] tracks = new int[override.length - 1];
            int trackCount = 0;
            for (int i = 0; i < override.length; i++) {
              if (override.getTrack(i) != trackIndex) {
                tracks[trackCount++] = override.getTrack(i);
              }
            }
            override = new TrackSelection(groupIndex, tracks);
          }
        } else {
          // Add the track to the override.
          int[] tracks = Arrays.copyOf(override.getTracks(), override.length + 1);
          tracks[tracks.length - 1] = trackIndex;
          override = new TrackSelection(groupIndex, tracks);
        }
      }
    }
    // Update the views with the new state.
    updateViews();
  }

  // Track name construction.

  private static String buildTrackName(Format format) {
    String trackName;
    if (MimeTypes.isVideo(format.sampleMimeType)) {
      trackName = joinWithSeparator(joinWithSeparator(buildResolutionString(format),
          buildBitrateString(format)), buildTrackIdString(format));
    } else if (MimeTypes.isAudio(format.sampleMimeType)) {
      trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(buildLanguageString(format),
          buildAudioPropertyString(format)), buildBitrateString(format)),
          buildTrackIdString(format));
    } else {
      trackName = joinWithSeparator(joinWithSeparator(buildLanguageString(format),
          buildBitrateString(format)), buildTrackIdString(format));
    }
    return trackName.length() == 0 ? "unknown" : trackName;
  }

  private static String buildResolutionString(Format format) {
    return format.width == Format.NO_VALUE || format.height == Format.NO_VALUE
        ? "" : format.width + "x" + format.height;
  }

  private static String buildAudioPropertyString(Format format) {
    return format.channelCount == Format.NO_VALUE || format.sampleRate == Format.NO_VALUE
        ? "" : format.channelCount + "ch, " + format.sampleRate + "Hz";
  }

  private static String buildLanguageString(Format format) {
    return TextUtils.isEmpty(format.language) || "und".equals(format.language) ? ""
        : format.language;
  }

  private static String buildBitrateString(Format format) {
    return format.bitrate == Format.NO_VALUE ? ""
        : String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f);
  }

  private static String joinWithSeparator(String first, String second) {
    return first.length() == 0 ? second : (second.length() == 0 ? first : first + ", " + second);
  }

  private static String buildTrackIdString(Format format) {
    return format.id == null ? "" : " (" + format.id + ")";
  }

}
