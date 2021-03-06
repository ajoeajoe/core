/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.libreoffice.LOKitShell;
import org.libreoffice.canvas.GraphicSelectionCanvasElement;
import org.libreoffice.canvas.GraphicSelectionHandleCanvasElement;
import org.mozilla.gecko.gfx.ImmutableViewportMetrics;
import org.mozilla.gecko.gfx.LayerView;
import org.mozilla.gecko.gfx.RectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Text cursor view responsible to show the cursor drawable on the screen.
 */
public class TextCursorView extends View implements View.OnTouchListener {
    private static final String LOGTAG = TextCursorView.class.getSimpleName();
    private static final float CURSOR_WIDTH = 2f;
    private static final int CURSOR_BLINK_TIME = 500;

    private boolean mInitialized = false;
    private RectF mCursorPosition = new RectF();
    private RectF mCursorScaledPosition = new RectF();
    private Paint mCursorPaint = new Paint();
    private int mCursorAlpha = 0;
    private boolean mCursorVisible;

    private List<RectF> mSelections = new ArrayList<RectF>();
    private List<RectF> mScaledSelections = new ArrayList<RectF>();
    private Paint mSelectionPaint = new Paint();
    private boolean mSelectionsVisible;

    private Paint mGraphicSelectionPaint = new Paint();

    private GraphicSelectionCanvasElement mGraphicSelection;

    private PointF mTouchStart = new PointF();
    private PointF mDeltaPoint = new PointF();

    private boolean mGraphicSelectionVisible;
    private boolean mGraphicSelectionMove = false;

    private LayerView mLayerView;

    private GraphicSelectionHandleCanvasElement mHandles[] = new GraphicSelectionHandleCanvasElement[8];
    private GraphicSelectionHandleCanvasElement mDragHandle = null;

    public TextCursorView(Context context) {
        super(context);
        initialize();
    }

    public TextCursorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public TextCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    /**
     * Initialize the selection and cursor view.
     */
    private void initialize() {
        if (!mInitialized) {
            setOnTouchListener(this);

            mCursorPaint.setColor(Color.BLACK);
            mCursorPaint.setAlpha(0xFF);
            mCursorVisible = false;

            mSelectionPaint.setColor(Color.BLUE);
            mSelectionPaint.setAlpha(50);
            mSelectionsVisible = false;

            mGraphicSelectionPaint.setStyle(Paint.Style.STROKE);
            mGraphicSelectionPaint.setColor(Color.BLACK);
            mGraphicSelectionPaint.setStrokeWidth(2);

            mGraphicSelection = new GraphicSelectionCanvasElement(mGraphicSelectionPaint);

            mGraphicSelectionVisible = false;

            mHandles[0] = new GraphicSelectionHandleCanvasElement(mGraphicSelectionPaint);
            mHandles[1] = new GraphicSelectionHandleCanvasElement(mGraphicSelectionPaint);
            mHandles[2] = new GraphicSelectionHandleCanvasElement(mGraphicSelectionPaint);
            mHandles[3] = new GraphicSelectionHandleCanvasElement(mGraphicSelectionPaint);
            mHandles[4] = new GraphicSelectionHandleCanvasElement(mGraphicSelectionPaint);
            mHandles[5] = new GraphicSelectionHandleCanvasElement(mGraphicSelectionPaint);
            mHandles[6] = new GraphicSelectionHandleCanvasElement(mGraphicSelectionPaint);
            mHandles[7] = new GraphicSelectionHandleCanvasElement(mGraphicSelectionPaint);

            postDelayed(cursorAnimation, CURSOR_BLINK_TIME);

            mInitialized = true;
        }
    }

    public void changeCursorPosition(RectF position) {
        LayerView layerView = LOKitShell.getLayerView();
        if (layerView == null) {
            Log.e(LOGTAG, "Can't position cursor because layerView is null");
            return;
        }

        mCursorPosition = position;

        ImmutableViewportMetrics metrics = layerView.getViewportMetrics();
        repositionWithViewport(metrics.viewportRectLeft, metrics.viewportRectTop, metrics.zoomFactor);
    }

    public void changeSelections(List<RectF> selectionRects) {
        LayerView layerView = LOKitShell.getLayerView();
        if (layerView == null) {
            Log.e(LOGTAG, "Can't position selections because layerView is null");
            return;
        }

        mSelections = selectionRects;

        ImmutableViewportMetrics metrics = layerView.getViewportMetrics();
        repositionWithViewport(metrics.viewportRectLeft, metrics.viewportRectTop, metrics.zoomFactor);
    }

    public void changeGraphicSelection(RectF rectangle) {
        LayerView layerView = LOKitShell.getLayerView();
        if (layerView == null) {
            Log.e(LOGTAG, "Can't position selections because layerView is null");
            return;
        }

        mGraphicSelection.mRectangle = rectangle;

        ImmutableViewportMetrics metrics = layerView.getViewportMetrics();
        repositionWithViewport(metrics.viewportRectLeft, metrics.viewportRectTop, metrics.zoomFactor);
    }

    public void repositionWithViewport(float x, float y, float zoom) {
        mCursorScaledPosition = convertPosition(mCursorPosition, x, y, zoom);
        mCursorScaledPosition.right = mCursorScaledPosition.left + CURSOR_WIDTH;

        mScaledSelections.clear();
        for (RectF selection : mSelections) {
            RectF scaledSelection = convertPosition(selection, x, y, zoom);
            mScaledSelections.add(scaledSelection);
        }

        RectF scaledGraphicSelection = convertPosition(mGraphicSelection.mRectangle, x, y, zoom);
        mGraphicSelection.reposition(scaledGraphicSelection);

        mHandles[0].reposition(scaledGraphicSelection.left, scaledGraphicSelection.top);
        mHandles[1].reposition(scaledGraphicSelection.centerX(), scaledGraphicSelection.top);
        mHandles[2].reposition(scaledGraphicSelection.right, scaledGraphicSelection.top);
        mHandles[3].reposition(scaledGraphicSelection.left, scaledGraphicSelection.centerY());
        mHandles[4].reposition(scaledGraphicSelection.right, scaledGraphicSelection.centerY());
        mHandles[5].reposition(scaledGraphicSelection.left, scaledGraphicSelection.bottom);
        mHandles[6].reposition(scaledGraphicSelection.centerX(), scaledGraphicSelection.bottom);
        mHandles[7].reposition(scaledGraphicSelection.right, scaledGraphicSelection.bottom);

        invalidate();
    }

    private RectF convertPosition(RectF cursorPosition, float x, float y, float zoom) {
        RectF cursor = RectUtils.scale(cursorPosition, zoom);
        cursor.offset(-x, -y);
        return cursor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mCursorVisible) {
            canvas.drawRect(mCursorScaledPosition, mCursorPaint);
        }
        if (mSelectionsVisible) {
            for (RectF selection : mScaledSelections) {
                canvas.drawRect(selection, mSelectionPaint);
            }
        }
        if (mGraphicSelectionVisible) {
            mGraphicSelection.draw(canvas);

            if (mGraphicSelectionMove) {
                for (GraphicSelectionHandleCanvasElement handle : mHandles) {
                    if (mDragHandle == handle) {
                        handle.drawSelected(canvas);
                    } else {
                        handle.draw(canvas);
                    }
                }
            } else {
                for (GraphicSelectionHandleCanvasElement handle : mHandles) {
                    handle.draw(canvas);
                }
            }
        }
    }

    private Runnable cursorAnimation = new Runnable() {
        public void run() {
            if (mCursorVisible) {
                mCursorPaint.setAlpha(mCursorPaint.getAlpha() == 0 ? 0xFF : 0);
                invalidate();
            }
            postDelayed(cursorAnimation, CURSOR_BLINK_TIME);
        }
    };

    public void showCursor() {
        mCursorVisible = true;
        invalidate();
    }

    public void hideCursor() {
        mCursorVisible = false;
        invalidate();
    }

    public void showSelections() {
        mSelectionsVisible = true;
        invalidate();
    }

    public void hideSelections() {
        mSelectionsVisible = false;
        invalidate();
    }

    public void showGraphicSelection() {
        mGraphicSelectionVisible = true;
        invalidate();
    }

    public void hideGraphicSelection() {
        mGraphicSelectionVisible = false;
        invalidate();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (mLayerView == null) {
            mLayerView = LOKitShell.getLayerView();
            if (mLayerView == null) {
                return false;
            }
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mTouchStart.x = event.getX();
                mTouchStart.y = event.getY();
                if (mGraphicSelectionVisible) {
                    return checkIfGraphicSelectionWasHit();
                }
            }
            case MotionEvent.ACTION_UP: {
                if (mGraphicSelectionVisible && mGraphicSelectionMove) {
                    mDeltaPoint.x = event.getX() - mTouchStart.x;
                    mDeltaPoint.y = event.getY() - mTouchStart.y;
                    return stopGraphicSelection();
                }
            }
            case MotionEvent.ACTION_MOVE: {
                if (mGraphicSelectionVisible && mGraphicSelectionMove) {
                    mDeltaPoint.x = event.getX() - mTouchStart.x;
                    mDeltaPoint.y = event.getY() - mTouchStart.y;
                    mGraphicSelection.dragging(new PointF(event.getX(), event.getY()));
                    invalidate();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkIfGraphicSelectionWasHit() {
        // Check if handle was hit
        mDragHandle = null;
        for (GraphicSelectionHandleCanvasElement handle : mHandles) {
            if (handle.contains(mTouchStart.x, mTouchStart.y)) {
                mDragHandle = handle;
                mGraphicSelectionMove = true;
                mGraphicSelection.dragStart(GraphicSelectionCanvasElement.DragType.EXTEND, mTouchStart);
                sendGraphicSelectionStart(handle.mPosition);
                return true;
            }
        }
        // Check if inside graphic selection was hit
        if (mGraphicSelection.contains(mTouchStart.x, mTouchStart.y)) {
            mGraphicSelectionMove = true;
            mGraphicSelection.dragStart(GraphicSelectionCanvasElement.DragType.MOVE, mTouchStart);
            sendGraphicSelectionStart(mTouchStart);
            return true;
        }
        return false;
    }

    private boolean stopGraphicSelection() {
        mGraphicSelectionMove = false;

        PointF point = new PointF();
        if (mDragHandle != null) {
            point.x = mDragHandle.mPosition.x;
            point.y = mDragHandle.mPosition.y;
        } else {
            point.x = mTouchStart.x;
            point.y = mTouchStart.y;
        }
        point.offset(mDeltaPoint.x, mDeltaPoint.y);
        sendGraphicSelectionEnd(point);

        mGraphicSelection.dragEnd();
        invalidate();
        return true;
    }

    private void sendGraphicSelectionStart(PointF screenPosition) {
        PointF documentPoint = mLayerView.getLayerClient().convertViewPointToLayerPoint(screenPosition);
        Log.i(LOGTAG, "Selection Start P: " + documentPoint + " : " + mGraphicSelection);
        LOKitShell.sendTouchEvent("GraphicSelectionStart", documentPoint);
    }

    private void sendGraphicSelectionEnd(PointF screenPosition) {
        PointF documentPoint = mLayerView.getLayerClient().convertViewPointToLayerPoint(screenPosition);
        Log.i(LOGTAG, "Selection End P: " + documentPoint + " : " + mGraphicSelection);
        LOKitShell.sendTouchEvent("GraphicSelectionEnd", documentPoint);
    }
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
