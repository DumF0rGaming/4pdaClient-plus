package org.softeg.slartus.forpdaplus.fragments.profile;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.softeg.slartus.forpdacommon.PatternExtensions;
import org.softeg.slartus.forpdaplus.App;
import org.softeg.slartus.forpdaplus.Client;
import org.softeg.slartus.forpdaplus.MainActivity;
import org.softeg.slartus.forpdaplus.R;
import org.softeg.slartus.forpdaplus.classes.AdvWebView;
import org.softeg.slartus.forpdaplus.classes.HtmlBuilder;
import org.softeg.slartus.forpdaplus.classes.SaveHtml;
import org.softeg.slartus.forpdaplus.common.AppLog;
import org.softeg.slartus.forpdaplus.fragments.WebViewFragment;
import org.softeg.slartus.forpdaplus.prefs.Preferences;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Created by radiationx on 07.11.15.
 */
public class ProfileEditFragment extends WebViewFragment {
    private Menu menu;
    private AdvWebView m_WebView;
    private Handler mHandler = new android.os.Handler();
    public final static String m_Title = "Изменить личные данные";
    private String parentTag = App.getInstance().getCurrentFragmentTag();
    private final static String url = "http://4pda.ru/forum/index.php?act=UserCP&CODE=01";
    View view;

    public static void editProfile(){
        MainActivity.addTab(m_Title, url, new ProfileEditFragment());
    }


    @Override
    public AdvWebView getWebView() {
        return m_WebView;
    }


    @Override
    public View getView() {
        return view;
    }

    @Override
    public WebViewClient MyWebViewClient() {
        return new WebViewClient();
    }

    @Override
    public String getTitle() {
        return m_Title;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void reload() {}

    @Override
    public boolean closeTab() {
        return false;
    }

    @Override
    public Menu getMenu() {
        return menu;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.profile_edit_activity, container, false);
        setHasOptionsMenu(true);
        assert view != null;
        m_WebView = (AdvWebView) view.findViewById(R.id.wvBody);
        registerForContextMenu(m_WebView);

        m_WebView.getSettings();
        m_WebView.getSettings().setDomStorageEnabled(true);
        m_WebView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        m_WebView.getSettings().setAppCachePath(getActivity().getApplicationContext().getCacheDir().getAbsolutePath());
        m_WebView.getSettings().setAppCacheEnabled(true);

        m_WebView.getSettings().setAllowFileAccess(true);

        m_WebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        m_WebView.addJavascriptInterface(this, "HTMLOUT");
        m_WebView.getSettings().setDefaultFontSize(Preferences.Topic.getFontSize());

        getEditProfileTask task = new getEditProfileTask(getActivity());
        task.execute("".replace("|", ""));

        if (Preferences.System.isDevSavePage()|
                Preferences.System.isDevInterface()|
                Preferences.System.isDevStyle())
            Toast.makeText(getActivity(), "Режим разработчика", Toast.LENGTH_SHORT).show();
        return view;
    }

    @JavascriptInterface
    public void saveHtml(final String html) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SaveHtml(getActivity(), html, "EditProfile");
            }
        });
    }

    public void saveHtml() {
        try {
            m_WebView.loadUrl("javascript:window.HTMLOUT.saveHtml('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
        } catch (Throwable ex) {
            AppLog.e(getActivity(), ex);
        }
    }

    @JavascriptInterface
    public void sendProfile(final String json) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("asdasd", json);
                new editProfileTask(getActivity(), json).execute();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item;
        if (Preferences.System.isDevSavePage()) {
            menu.add("Сохранить страницу").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem menuItem) {
                    try {
                        saveHtml();
                    } catch (Exception ex) {
                        return false;
                    }
                    return true;
                }
            });
        }
        item = menu.add(R.string.Close).setIcon(R.drawable.ic_close_white_24dp);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                getActivity().finish();
                return true;
            }
        });
        this.menu = menu;
    }

    private class editProfileTask extends AsyncTask<String, Void, Void> {
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        MaterialDialog dialog;

        public editProfileTask(Context context,String json) {
            dialog = new MaterialDialog.Builder(context)
                    .progress(true, 0)
                    .content("Отправка данных")
                    .build();

            try {
                JSONArray jr = new JSONArray(json);
                JSONObject jb;
                for (int i = 0; i <jr.length(); i++){
                    jb = (JSONObject)jr.getJSONObject(i);
                    if(!jb.getString("name").equals("null")){
                        additionalHeaders.put(jb.getString("name"), jb.getString("value"));
                    }
                }
                additionalHeaders.put("auth_key", Client.getInstance().getAuthKey());
                additionalHeaders.put("act", "UserCP");
                additionalHeaders.put("CODE", "21");
                additionalHeaders.put("ed-0_wysiwyg_used", "0");
                additionalHeaders.put("editor_ids[]", "ed-0");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            try {
                this.dialog.show();
            } catch (Exception ex) {
                this.cancel(true);
            }
        }

        @Override
        protected Void doInBackground(String... urls) {
            try {
                Client.getInstance().performPost("http://4pda.ru/forum/index.php", additionalHeaders);
            } catch (IOException e) {
                Log.d("asdasd", e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            Toast.makeText(getContext(),"Данные отправлены",Toast.LENGTH_SHORT).show();
            ((ProfileFragment)((MainActivity)getActivity())
                    .getSupportFragmentManager()
                    .findFragmentByTag(parentTag)).startLoadData();
            ((MainActivity) getActivity()).removeTab(getTag());
            MainActivity.selectTabByTag(parentTag);
        }
    }

    private void showThemeBody(String body) {
        try {
            getActivity().setTitle(m_Title);
            m_WebView.loadDataWithBaseURL("http://4pda.ru/forum/", body, "text/html", "UTF-8", null);
        } catch (Exception ex) {
            AppLog.e(getActivity(), ex);
        }
    }

    private class getEditProfileTask extends AsyncTask<String, String, Boolean> {
        private final MaterialDialog dialog;

        public getEditProfileTask(Context context) {
            dialog = new MaterialDialog.Builder(context)
                    .progress(true, 0)
                    .content("Загрузка")
                    .build();
        }

        private String m_ThemeBody;

        @Override
        protected Boolean doInBackground(String... forums) {
            try {
                if (isCancelled()) return false;
                Client client = Client.getInstance();
                m_ThemeBody = transformBody(client.performGet(url));

                return true;
            } catch (Throwable e) {
                return false;
            }
        }

        private String transformBody(String body) {
            HtmlBuilder builder = new HtmlBuilder();
            builder.beginHtml(m_Title);
            builder.beginBody("edit_profile");

            builder.append(parseBody(body));

            builder.endBody();
            builder.endHtml();
            return builder.getHtml().toString();
        }

        private String parseBody(String body) {
            Matcher m = PatternExtensions.compile("br \\/>\\s*(<fieldset>[\\S\\s]*<.form>)").matcher(body);
            if (m.find()) {
                body = "<form>"+m.group(1);
                //body =  + "</form><input type=\"button\" value=\"asdghjk\" onclick=\"jsonElem();\">";
                body = body.replaceAll("<td class=\"row1\" width=\"30%\"><b>О себе:</b>[\\s\\S]*?</td>",
                        "<td class=\"row1\" width=\"30%\"><b>О себе</b></td>");
                body = body.replaceAll("<td width=\"30%\" class=\"row1\" style='padding:6px;'><b>Город</b>[\\s\\S]*?</td>",
                        "<td class=\"row1\" width=\"30%\" style='padding:6px;'><b>Город</b></td>");
                body = body.replaceAll("legend","h2").replaceAll("<fieldset>","<div class=\"field\">").replaceAll("</fieldset>","</div>");
                Document doc = Jsoup.parse(body);
                doc.select(".formbuttonrow .button").remove();
                doc.select(".formbuttonrow").append("<input type=\"button\" value=\"Сохранить\" onclick=\"jsonElem();\">");
                doc.select("textarea").first().attr("maxlength","500");
                body = doc.html();
            }
            return body;
        }

        @Override
        protected void onProgressUpdate(final String... progress) {
            mHandler.post(new Runnable() {
                public void run() {
                    dialog.setContent(progress[0]);
                }
            });
        }

        protected void onPreExecute() {
            try {
                this.dialog.show();
            } catch (Exception ex) {
                this.cancel(true);
            }
        }

        private Throwable ex;

        protected void onPostExecute(final Boolean success) {
            try {
                if (this.dialog.isShowing()) {
                    this.dialog.dismiss();
                }
            } catch (Exception ex) {
                Log.e("!!@@#!", ex.toString());
            }

            if (isCancelled()) return;

            if (success) {
                showThemeBody(m_ThemeBody);
            } else {
                getSupportActionBar().setTitle(ex.getMessage());
                m_WebView.loadDataWithBaseURL("\"file:///android_asset/\"", m_ThemeBody, "text/html", "UTF-8", null);
                AppLog.e(getActivity(), ex);
            }

            CookieSyncManager syncManager = CookieSyncManager.createInstance(m_WebView.getContext());
            CookieManager cookieManager = CookieManager.getInstance();
            try {
                for (Cookie cookie : Client.getInstance().getCookies()) {

                    if (cookie.getDomain() != null) {
                        Log.d("asdas!", cookie.getDomain() + " " + cookie.getValue());
                        cookieManager.setCookie(cookie.getDomain(), cookie.getName() + "=" + cookie.getValue());
                    }
                    //cookieManager.setCookie(cookie.getTitle(),cookie.getValue());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            syncManager.sync();
        }
    }
}
