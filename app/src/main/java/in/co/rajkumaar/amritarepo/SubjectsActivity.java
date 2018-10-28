package in.co.rajkumaar.amritarepo;

import android.Manifest;
import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SubjectsActivity extends AppCompatActivity {

    String href;
    String externLink;
    List<String> assessments = new ArrayList<>();
    int statusCode;
    String proxy;
    List<String> links = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        proxy=getString(R.string.proxyurl);
        String protocol=getString(R.string.protocol);
        String cloudSpace=getString(R.string.clouDspace);
        String amrita=getString(R.string.amrita);
        String port=getString(R.string.port);

        externLink=protocol+cloudSpace+amrita+port;
        TextView textView=findViewById(R.id.empty_view);
        textView.setVisibility(View.GONE);
        ImageView imageView=findViewById(R.id.empty_imageview);
        imageView.setVisibility(View.GONE);
        TextView wifiwarning=findViewById(R.id.wifiwarning);
        wifiwarning.setVisibility(View.GONE);
        Bundle bundle = getIntent().getExtras();
        href = "" + bundle.get("href");
        this.setTitle(""+bundle.get("pageTitle"));
        new clearCache().clear();
        new Load().execute();
    }
    private class Load extends AsyncTask<Void, Void, Void> {
        Document nextDoc,document=null;
        @Override
        protected Void doInBackground(Void... voids) {
            assessments.clear();
            links.clear();
            try {
                // Connect to the web site
                statusCode=Jsoup.connect(href).execute().statusCode();
                document = Jsoup.connect(href).get();
            } catch (IOException e) {

                try {
                    document = (Jsoup.connect(proxy).method(Connection.Method.POST).data("data", href).execute().parse());
                    statusCode = (Jsoup.connect(proxy).method(Connection.Method.POST).data("data", href).execute().statusCode());
                }catch (IOException v){
                    v.printStackTrace();
                }
                e.printStackTrace();

            }
            finally

            {
                try {
                    if (document != null) {
                        Elements elements = document.select("div[xmlns=http://di.tamu.edu/DRI/1.0/]").get(0).select("ul").get(0).select("li").get(0).select("a[href]");
                        String nextUrl = externLink + elements.get(0).attr("href");
                        nextDoc = null;
                        try {
                            nextDoc = Jsoup.connect(nextUrl).get();

                        } catch (IOException ex) {
                            try {
                                nextDoc = (Jsoup.connect(proxy).method(Connection.Method.POST).data("data", nextUrl).execute().parse());
                            } catch (IOException r) {
                                r.printStackTrace();
                            }

                            ex.printStackTrace();
                        }
                    }
                }catch (NullPointerException e){
                    e.printStackTrace();
            }catch(IndexOutOfBoundsException e){
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try{
            if(nextDoc!=null){
                Elements nextLinks = nextDoc.select("div[id=aspect_artifactbrowser_ItemViewer_div_item-view]").get(0).select("div[xmlns:i18n=http://apache.org/cocoon/i18n/2.1]").select("a[href]");
                Elements nextElements = nextDoc.select("div[id=aspect_artifactbrowser_ItemViewer_div_item-view]").get(0).select("span[xmlns:i18n=http://apache.org/cocoon/i18n/2.1]");
                for (int i = 0; i < nextElements.size(); ++i)
                    if (!nextElements.get(i).attr("title").isEmpty()) {
                        assessments.add(nextElements.get(i).attr("title"));
                    }
                for (int i = 0; i < nextLinks.size(); i += 2)
                    links.add(nextLinks.get(i).attr("href"));
            }}catch (IndexOutOfBoundsException e)
            {
                e.printStackTrace();
                Toast.makeText(SubjectsActivity.this,"Some error occurred. Please report to the developer.",Toast.LENGTH_LONG).show();
                SubjectsActivity.this.finish();
            }
            ProgressBar progressBar = findViewById(R.id.loading_indicator);
            progressBar.setVisibility(View.GONE);
            if(statusCode!=200)
            {
                TextView emptyView=findViewById(R.id.empty_view);
                emptyView.setVisibility(View.VISIBLE);
                ImageView imageView=findViewById(R.id.empty_imageview);
                imageView.setVisibility(View.VISIBLE);
                TextView wifiwarning=findViewById(R.id.wifiwarning);
                wifiwarning.setVisibility(View.VISIBLE);
            }
            else
            {
                TextView emptyView=findViewById(R.id.empty_view);
                emptyView.setVisibility(View.GONE);
                ImageView imageView=findViewById(R.id.empty_imageview);
                imageView.setVisibility(View.GONE);
                TextView wifiwarning=findViewById(R.id.wifiwarning);
                wifiwarning.setVisibility(View.GONE);
            ListView listView = findViewById(R.id.list);
            ArrayAdapter<String> semsAdapter = new ArrayAdapter<String>(SubjectsActivity.this, android.R.layout.simple_list_item_1, assessments);
            listView.setAdapter(semsAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    final ArrayList<String> qPaperOptions=new ArrayList();
                    qPaperOptions.add("Open");
                    qPaperOptions.add("Download");
                    final View viewLocal=view;
                    final int p=i;
                    AlertDialog.Builder qPaperBuilder=new AlertDialog.Builder(SubjectsActivity.this);
                    ArrayAdapter<String> optionsAdapter = new ArrayAdapter<String>(SubjectsActivity.this, android.R.layout.simple_list_item_1, qPaperOptions);
                   qPaperBuilder.setAdapter(optionsAdapter, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int pos) {
                           if(pos==0) {
                               String link=links.get(p).substring(0, links.get(p).indexOf("?"));
                               new OpenTask(SubjectsActivity.this,externLink+link,1);
                           }
                           else if(pos==1)
                           {
                               if (ContextCompat.checkSelfPermission(SubjectsActivity.this,
                                       Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                       != PackageManager.PERMISSION_GRANTED) {

                                   ActivityCompat.requestPermissions(SubjectsActivity.this,
                                           new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                           1);
                               }
                               else{
                                   if(isNetworkAvailable())
                                   {
                                       new DownloadTask(SubjectsActivity.this,externLink+links.get(p),1);
                                   }
                                   else{
                                       Snackbar.make(viewLocal,"Device not connected to Internet.",Snackbar.LENGTH_SHORT).show();
                                   }


                               }
                           }
                       }
                   });
                    qPaperBuilder.show();



                }
            });

            listView.setVisibility(View.VISIBLE);}
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
