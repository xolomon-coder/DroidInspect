package com.tanvir.m12poc;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class MainActivity extends Activity {
    private TextView status;
    private Button run;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 24, 20, 20);
        TextView heading = new TextView(this);
        heading.setText("M12 — Offline Roslyn Compiler Proof\nAndroid ARM64 / .NET 11");
        heading.setTextSize(20);
        root.addView(heading);
        run = new Button(this);
        run.setText("COMPILE EDITED C# FIXTURE");
        root.addView(run);
        status = new TextView(this);
        status.setTextSize(14);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(status);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        run.setOnClickListener(v -> compileFixture());
        status.setText("This isolated proof compiles a bundled edited C# source file offline. It does not modify DroidInspect production files.");
    }

    private void compileFixture() {
        run.setEnabled(false);
        status.setText("Preparing source and .NET 11 reference assemblies…");
        new Thread(() -> {
            String result;
            try {
                File base = new File(getFilesDir(), "m12-compiler-proof");
                File source = new File(base, "source");
                File refs = new File(base, "references");
                if (!source.exists() && !source.mkdirs()) throw new Exception("Cannot create source folder.");
                if (!refs.exists() && !refs.mkdirs()) throw new Exception("Cannot create reference folder.");
                copyAsset("fixture/EditedFixture.cs", new File(source, "EditedFixture.cs"));
                String[] names = getAssets().list("reference-assemblies/net11.0");
                if (names == null || names.length == 0) throw new Exception("Reference assemblies are missing.");
                for (String name : names) {
                    if (name.endsWith(".dll")) copyAsset("reference-assemblies/net11.0/" + name, new File(refs, name));
                }
                File output = new File(base, "DroidInspect.M12.EditedFixture.dll");
                result = NativeBridge.compile(source.getAbsolutePath(), output.getAbsolutePath(), refs.getAbsolutePath(), "");
                if (result.startsWith("M12 COMPILATION PASS")) {
                    if (!output.isFile() || output.length() < 512) {
                        result += "\nJAVA VERIFY FAIL: output file missing or too small.";
                    } else {
                        try (FileInputStream check = new FileInputStream(output)) {
                            int first = check.read();
                            int second = check.read();
                            if (first != 'M' || second != 'Z')
                                result += "\nJAVA VERIFY FAIL: output does not begin with the MZ signature.";
                            else
                                result += "\nJAVA VERIFY PASS: MZ signature; " + output.length() + " bytes.";
                        }
                    }
                }
            } catch (Throwable ex) {
                result = "M12 DEVICE PROOF ERROR: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
            }
            final String message = result;
            runOnUiThread(() -> {
                status.setText(message);
                run.setEnabled(true);
            });
        }).start();
    }

    private void copyAsset(String asset, File target) throws Exception {
        if (target.isFile() && target.length() > 0) return;
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new Exception("Cannot create " + parent);
        try (InputStream in = getAssets().open(asset); OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            out.flush();
        }
    }
}
