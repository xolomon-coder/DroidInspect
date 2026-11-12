package com.tanvir.m11poc;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import java.io.*;
import java.util.concurrent.*;
public final class MainActivity extends Activity {
 private static final int PICK=41;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private Button select; private TextView status,source;
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);select=findViewById(R.id.selectAssembly);status=findViewById(R.id.status);source=findViewById(R.id.source);select.setOnClickListener(v->pick());try{Class.forName("com.tanvir.m11poc.NativeBridge");status.setText("Native engine loaded. Offline decompilation ready.");}catch(Throwable e){status.setText("Native load failed: "+e);select.setEnabled(false);}}
 private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK);}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r!=PICK||c!=RESULT_OK||d==null||d.getData()==null)return;Uri uri=d.getData();select.setEnabled(false);status.setText("Copying input and preparing offline references...");worker.execute(()->{File input=null;try{input=File.createTempFile("m11-",".dll",getCacheDir());try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(input)){if(in==null)throw new IOException("Cannot open selected document");byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}File refs=new File(getFilesDir(),"reference-assemblies/net11.0");copyAssets("reference-assemblies/net11.0",refs);String result=NativeBridge.decompileFile(input.getAbsolutePath(),refs.getAbsolutePath());runOnUiThread(()->{source.setText(result);status.setText(result.startsWith("M11 DECOMPILATION ERROR:")?"Decompilation failed.":"ILSpy returned C# source text.");});}catch(Throwable e){runOnUiThread(()->status.setText("Error: "+e));}finally{if(input!=null)input.delete();runOnUiThread(()->select.setEnabled(true));}});}
 private void copyAssets(String path,File dest)throws IOException{String[] old=dest.list();if(dest.isDirectory()&&old!=null&&old.length>0)return;if(!dest.exists()&&!dest.mkdirs())throw new IOException("Cannot create reference folder");String[] names=getAssets().list(path);if(names==null||names.length==0)throw new IOException("Reference pack missing from APK");for(String name:names){String child=path+"/"+name;String[] nested=getAssets().list(child);File target=new File(dest,name);if(nested!=null&&nested.length>0)copyAssets(child,target);else try(InputStream in=getAssets().open(child);OutputStream out=new FileOutputStream(target)){byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}}}
 @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
