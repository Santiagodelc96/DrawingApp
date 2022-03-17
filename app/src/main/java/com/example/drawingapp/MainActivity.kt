package com.example.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


private const val FILE_NAME = "photo.jpg"
private lateinit var photoFile: File
class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null

    //Todo: create an ActivityResultLauncher with MultiplePermissions for requesting both read and write external storage and access the camera
    val requestPermission: ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted){
                    when(permissionName){
                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
                            Toast.makeText(this,"Permission granted for READ EXTERNAL STORAGE",Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this,"Permission granted for WRITING EXTERNAL STORAGE",Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{ //Todo: Displaying another toast if permission is not granted and this time focus on Read external storage
                    when(permissionName){
                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
                            Toast.makeText(this,"Permission denied for READ EXTERNAL STORAGE",Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this,"Permission denied for WRITING EXTERNAL STORAGE",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    //Todo: create an ActivityResultLauncher with MultiAction for do both read and write external storage and the camera action
    val requestAction: ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ actionRequested ->
        actionRequested.entries.forEach{
            val actionRequestedName = it.key
            val isGranted = it.value
            if(isGranted){
                when(actionRequestedName){
                    Manifest.permission.READ_EXTERNAL_STORAGE -> {
                        val picIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(picIntent)
                    }
                    else -> {
                        if(isReadStorageAllowed()){
                            showProgressDialog()
                            lifecycleScope.launch{
                                val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                                val flDrawingViewBitmap: Bitmap = getBitmapFromView(flDrawingView)
                                saveBitmapFile(flDrawingViewBitmap)
                            }
                        }
                    }
                }
            }else{ //Todo: Displaying another toast if permission is not granted and this time focus on Read external storage
                when(actionRequestedName){
                    Manifest.permission.READ_EXTERNAL_STORAGE -> {
                        Toast.makeText(this,"Permission denied for READ EXTERNAL STORAGE",Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this,"Permission denied for WRITING EXTERNAL STORAGE",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)
        drawingView?.setSizeForBrush(20.toFloat())

        requestAllPermissions()

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[9] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))

        val brushBtn: ImageButton = findViewById(R.id.ib_brush)
        brushBtn.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        //TODO: (Adding an click event to image button for selecting the image from gallery.)
        val galleryButton: ImageButton = findViewById(R.id.ib_image_selector)
        galleryButton.setOnClickListener {
            requestReadAction()
        }

        //TODO: (Adding an click event to image button to open the camera and take a picture.)
        val undoButton: ImageButton = findViewById(R.id.ib_undo)
        undoButton.setOnClickListener {
            drawingView?.onClickUndo()
        }

        //TODO: (Adding an click event to image button to save the drawing in the the phone memory.)
        val saveButton: ImageButton = findViewById(R.id.ib_save)
        saveButton.setOnClickListener {
            requestWriteAction()
        }
    }

    //TODO: Painting Methods
    //Method is used to launch the dialog to select different brush sizes.
    private fun showBrushSizeChooserDialog(){
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val bigBtn: ImageButton = brushDialog.findViewById(R.id.ib_big_brush)
        bigBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    // Method is called when color is clicked from pallet_normal. @param view ImageButton on which click took place.
    fun paintClicked(view: View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton // Update the color
            // Here the tag is used for swapping the current color with previous color.
            val colorTag = imageButton.tag.toString() // The tag stores the selected view
            drawingView?.setColor(colorTag) // The color is set as per the selected tag here.
            imageButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed)) // Swap the backgrounds for last active and currently active image button.
            mImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_normal))
            mImageButtonCurrentPaint = view //Current view is updated with selected view in the form of ImageButton.
        }
    }

    //Create rationale dialog Shows rationale dialog for displaying why the app needs permission. Only shown if the user has denied the permission request previously
    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    //Method that allows to request all the app permissions:
    private fun requestAllPermissions(){
        //Check if the permission was denied and show rationale
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE) &&
            ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
            ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)){
            //call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog("Kids Drawing App", "Kids drawing app needs to access your external storage, and camera.")
        }else{
            //if it has not been denied then request for permission
            //he registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    //TODO: READ EXTERNAL STORAGE
    //Method is used to launch the WRITE EXTERNAL STORAGE and save all the drawing in the phone memory.
    private fun requestReadAction(){
        //Check if the permission was denied and show rationale
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            //call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog("Kids Drawing App", "Kids drawing app needs to access your external storage.")
        }else{
            //if it has not been denied then request for permission
            //he registered ActivityResultCallback gets the result of this request.
            requestAction.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    //Activity result launcher that allows to open the gallery to select a background picture.
    val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK && result.data!=null){ //Evaluates if the result variable info was carried correctly or not.
                val imageBackground: ImageView = findViewById(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
            }else {
                Toast.makeText(this, "Gallery picture information could not be carried", Toast.LENGTH_SHORT).show() //Show a Toast.
            }
        }

    //Method that allows to check the READ_EXTERNAL_STORAGE permission.
    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    //TODO: WRITE EXTERNAL STORAGE:

    //Method that allows to get a Bitmap from a view:
    private fun getBitmapFromView(view: View): Bitmap{
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888) //Create a Bitmap with the same size of the view.
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap) //Generate a Canvas that host the draw class (writing into a bitmap) with the view information.
        val backgroundDrawable = view.background //View background is saved since is another layer of the drawing layer.
        if(backgroundDrawable != null){ //Check if the background is empty or if it has a picture background.
            backgroundDrawable.draw(canvas) //Get the background drawable, then draw it on the canvas
        }else{
            canvas.drawColor(Color.WHITE) //If it does not have background drawable, then draw white background on the canvas
        }
        view.draw(canvas) //Write the Bitmap of the view to add the Views inside of the Frame Layout and make it one view. (Draw the view on the canvas)
        return  returnedBitmap
    }

    //Method to save the Drawing:
    private suspend fun saveBitmapFile(mBitmap: Bitmap): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream() // The buffer capacity is initially 32 bytes, though its size increases if necessary.
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90, bytes)
                    /**
                     * Write a compressed version of the bitmap to the specified outputstream.
                     * If this returns true, the bitmap can be reconstructed by passing a
                     * corresponding inputstream to BitmapFactory.decodeStream(). Note: not
                     * all Formats support all bitmap configs directly, so it is possible that
                     * the returned bitmap from BitmapFactory could be in a different bitdepth,
                     * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
                     * pixels).
                     *
                     * @param format   The format of the compressed image
                     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
                     *                 small size, 100 meaning compress for max quality. Some
                     *                 formats, like PNG which is lossless, will ignore the
                     *                 quality setting
                     * @param stream   The outputstream to write the compressed data.
                     * @return true if successfully compressed to the specified stream.
                     */

                    val file = File(externalCacheDir?.absoluteFile.toString() + File.separator + "DrawingApp_" + System.currentTimeMillis() /1000 + ".png")
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.

                    val fileOutputStream = FileOutputStream(file) // Creates a file output stream to write to the file represented by the specified object.
                    fileOutputStream.write(bytes.toByteArray()) // Writes bytes from the specified byte array to this file output stream.
                    fileOutputStream.close() // Closes this file output stream and releases any system resources associated with this stream. This file output stream may no longer be used for writing bytes.

                    result = file.absolutePath // The file absolute path is return as a result.

                    //Switch from io to ui thread to show a toast
                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved successfully: $result",Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file",Toast.LENGTH_SHORT).show()
                        }
                    }
                }catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    //Method is used to launch the WRITE EXTERNAL STORAGE and save all the drawing in the phone memory.
    private fun requestWriteAction() {
        //Check if the permission was denied and show rationale
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            //call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog("Kids Drawing App", "Kids drawing app needs to access your camera")
        }else{
            //if it has not been denied then request for permission
            //he registered ActivityResultCallback gets the result of this request.
            requestAction.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    //TODO: Progress dialog
    //Method that allow to display a progress dialog:
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }

    //Method that cancel the progress Dialog:
    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    //TODO: Sharing the drawing
    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this,arrayOf(result),null){ path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }
}