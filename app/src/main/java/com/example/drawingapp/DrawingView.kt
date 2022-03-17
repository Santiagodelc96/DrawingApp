package com.example.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var mDrawPath: CustomPath? = null //An variable of CustomPath inner class to use it further.
    private var mCanvasBitmap: Bitmap? = null //An instance of the Bitmap.
    private var mDrawPaint: Paint? = null //The Paint class holds the style and color information about how to draw geometries, text and bitmaps.
    private var mCanvasPaint: Paint? = null //Instance of canvas paint view.
    private var mBrushSize: Float = 0.toFloat() //A variable for stroke/brush size to draw on the canvas.
    private var color = Color.BLACK //A variable to hold a color of the stroke.
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()
    /**
     * A variable for canvas which will be initialized later and used.
     *
     *The Canvas class holds the "draw" calls. To draw something, you need 4 basic components: A Bitmap to hold the pixels, a Canvas to host
     * the draw calls (writing into the bitmap), a drawing primitive (e.g. Rect,
     * Path, text, Bitmap), and a paint (to describe the colors and styles for the
     * drawing)
     */

    private var canvas: Canvas? = null

    init{
        setUpDrawing()
    }

    //This method initializes the attributes of the ViewForDrawing class.
    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color,mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE //This is to draw a STROKE style
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND //This is for store join
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND //This is for stroke Cap
        mCanvasPaint = Paint(Paint.DITHER_FLAG) //Paint flag that enables dithering when blitting.
        //mBrushSize = 20.toFloat() //A variable for stroke/brush size to draw on the canvas.
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    //This method is called when a stroke is drawn on the canvas as a part of the painting.
    //Note: Change Canvas to Canvas? if fails
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        //In this loop, all the drawed paths are saved in the mPaths ArrayList in order to keep showing them all.
        for(path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }

        if(!mDrawPath!!.isEmpty) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }

    }

    //This method acts as an event listener when a touch event is detected on the device.
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset() // Clear any lines and curves from the path, making it empty.
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX,touchY) // Set the beginning of the next contour to the point (x,y).
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX,touchY) // Add a line from the last point to the specified point (x,y).
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    fun onClickUndo(){
        if(mPaths.size > 0){
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate() //Calls the onDraw method.
        }
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float): android.graphics.Path() {

    }

}