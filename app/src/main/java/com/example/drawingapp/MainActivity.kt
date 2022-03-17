package com.example.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
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
import java.io.File


private const val FILE_NAME = "photo.jpg"
private lateinit var photoFile: File
class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null

    /// Todo 2: create an ActivityResultLauncher with MultiplePermissions since we are requesting both read and write
    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted){
                    when(permissionName){
                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
                            Toast.makeText(this,"Permission granted for READ EXTERNAL STORAGE",Toast.LENGTH_SHORT).show()
                            val picIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            openGalleryLauncher.launch(picIntent)
                        }
                        else -> {
                            Toast.makeText(this,"Permission granted for camera",Toast.LENGTH_SHORT).show()
                            photoFile = getPhotoFile(FILE_NAME)
                            val fileProvider = FileProvider.getUriForFile(this, "com.example.profilepicture.fileprovider", photoFile)
                            val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                .putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
                            if (camIntent.resolveActivity(this.packageManager) != null){
                                openCameraLauncher.launch(camIntent)
                            } else{
                                Toast.makeText(this,"Unable to open camera", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }else{ //Todo: Displaying another toast if permission is not granted and this time focus on Read external storage
                    when(permissionName){
                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
                            Toast.makeText(this,"Permission denied for READ EXTERNAL STORAGE",Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this,"Permission denied for camera",Toast.LENGTH_SHORT).show()
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
            requestStoragePermission()
        }

        //TODO: (Adding an click event to image button to open the camera and take a picture.)
        val undoButton: ImageButton = findViewById(R.id.ib_undo)
        undoButton.setOnClickListener {
            drawingView?.onClickUndo()
        }

        /*//TODO: (Adding an click event to image button to open the camera and take a picture.)
        val cameraButton: ImageButton = findViewById(R.id.ib_image_taker)
        cameraButton.setOnClickListener {
            requestCameraPermission()
        }*/
    }

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

    //Todo: create rationale dialog Shows rationale dialog for displaying why the app needs permission. Only shown if the user has denied the permission request previously
    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    //Read Storage usage:
    //Todo: create a method to requestStorage permission
    private fun requestStoragePermission(){
        //Check if the permission was denied and show rationale
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            //call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog("Kids Drawing App", "Kids drawing app needs to access your external storage")
        }else{
            //if it has not been denied then request for permission
            //he registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackground: ImageView = findViewById(R.id.iv_background)

                imageBackground.setImageURI(result.data?.data)
            }
        }

    //Camera usage functions:
    //Todo: create a method to CAMERA permission
    private fun requestCameraPermission() {
        //Todo: Check if the permission was denied and show rationale
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)){
            showRationaleDialog("Kids Drawing App", "Kids drawing app needs to access your camera")
        }else{
            // Todo: if it has not been denied then request for permission
            //  The registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    val openCameraLauncher:  ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if (result.resultCode == Activity.RESULT_OK){
                val imageBackground: ImageView = findViewById(R.id.iv_background)
                val userPicture = BitmapFactory.decodeFile(photoFile.absolutePath)
                val rotatedUserPicture =rotateBitmap(userPicture)
                imageBackground.setImageBitmap(rotatedUserPicture)
            } else {
                Toast.makeText(this, "Picture information could not be retreived", Toast.LENGTH_SHORT).show()
            }

        }

    private fun takePictureIntent(){
        photoFile = getPhotoFile(FILE_NAME)
        val fileProvider = FileProvider.getUriForFile(this, "com.example.profilepicture.fileprovider", photoFile)
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
        if (takePictureIntent.resolveActivity(this.packageManager) != null){
            openCameraLauncher.launch(takePictureIntent)
        } else{
            Toast.makeText(this,"Unable to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    fun rotateBitmap(source: Bitmap, degrees: Float = 90f): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90F)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }
}