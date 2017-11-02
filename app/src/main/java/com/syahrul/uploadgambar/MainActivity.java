package com.syahrul.uploadgambar;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Button upload, pilih;
    private ImageView review;
    private String data;
    private Uri UrlGambar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Dialog dialog;

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();//biar bisa uri file
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        upload = (Button)findViewById(R.id.btn_upload);
        pilih = (Button)findViewById(R.id.pilih_gambar);
        review = (ImageView)findViewById(R.id.rev_upload);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadGambar();
            }
        });
        pilih.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File imagesFolder = null; //deklarasi file
                try {
                    imagesFolder = saveFile("gejala",".jpeg");
                   // imagesFolder.delete();
                }catch (Exception e){
                    e.printStackTrace();
                }
                file = imagesFolder;
                uriGambar = Uri.fromFile(imagesFolder);
                imageIntent.putExtra(MediaStore.EXTRA_OUTPUT,uriGambar);

                startActivityForResult(imageIntent,0);
            }
        });
    }
    public void tampilHasil(){
        this.getContentResolver().notifyChange(uriGambar, null);
        ContentResolver cr = this.getContentResolver();
        Bitmap bitmap;
        try
        {
            bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, uriGambar);
            review.setImageBitmap(bitmap);
        }
        catch (Exception e)
        {
            Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
        }
    }
    private Uri uriGambar;
    private File file;

    public File saveFile(String namaFile,String extensi) throws Exception {//kalo gak pake try catch
        File file = Environment.getExternalStorageDirectory();
        file = new File(file.getAbsoluteFile()+"/gejalafoto/");//path nyimpen
        if(file.exists() == false)
            file.mkdirs();
            file = File.createTempFile(namaFile,extensi,file);
        return file;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            uploadGambar();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage()+"erro diinputster", Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);//mendetek ke galeri
        Uri contentUri = Uri.fromFile(file);
        intent.setData(contentUri);
        sendBroadcast(intent);
        tampilHasil();
    }
    public void uploadGambar(){
        final ProgressDialog progressDialog = new ProgressDialog(this);
        new AsyncTask<Void,Void,String>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setMessage("Loading....");
                progressDialog.show();
            }

            @Override
            protected String doInBackground(Void... voids) {
              return  uploadKeServer(uriGambar.getPath(),MainActivity.this,
                      uriGambar,"df");
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }


    public String uploadKeServer(String file, Context context, Uri uri,String idpesan){
        HttpURLConnection conn = null;
        String tipefile = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String respon = null;
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 512000;
        try {
            URL url = new URL("http://192.168.100.2/upload-mobile/simpangambar.php");
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            dos = new DataOutputStream(conn.getOutputStream());
            ByteArrayInputStream fileInputStream=null;
            File sourceFile = new File(Environment.getExternalStorageDirectory()+file);
            fileInputStream = bitmapToStream(uri);
            Log.d("ukuran gambar",String.valueOf(fileInputStream.available()));
            String param = "type="+tipefile;
            dos.writeBytes(twoHyphens + boundary + lineEnd);

            dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\""
                    +file+"\""+lineEnd+"");

            dos.writeBytes(lineEnd);

            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            Log.d("ukuran buffer ",String.valueOf(bufferSize));
            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                //Log.d("ukuran buffer -- ",String.valueOf(bufferSize)+" - "+String.valueOf(bytesAvailable));
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                //Log.d("byte read",String.valueOf(bytesRead));
            }
            //Log.d("ukuran buffer terakhir ",String.valueOf(fileInputStream.available()));
            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + lineEnd);

            dos.writeBytes("Content-Disposition: form-data; name=\"tipe\""+lineEnd+"");
            dos.writeBytes(lineEnd);
            dos.writeBytes("df");
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + lineEnd);

            dos.writeBytes("Content-Disposition: form-data; name=\"idpesan\""+lineEnd+"");
            dos.writeBytes(lineEnd);
            dos.writeBytes("dfddfdfafd");
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens);

            fileInputStream.close();
            dos.flush();
            dos.close();
            BufferedReader buf = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = buf.readLine()) != null) {
                sb.append(line);
            }
            buf.close();;
            respon = sb.toString();
        }catch (Exception e){
            respon = e.getMessage();
        }
        return respon;
    }
    public ByteArrayInputStream bitmapToStream(Uri uri){
        Bitmap bitmap=null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            bitmap = Bitmap.createScaledBitmap(bitmap,648,1152,
                    true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
        return bs;
    }
}
