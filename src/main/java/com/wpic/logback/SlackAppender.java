package com.wpic.logback;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class SlackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private final static String API_URL = "https://slack.com/api/chat.postMessage";

    private String token;

    private String userName;

    private String iconEmoji;

    private String channel;

    private Layout layout;

    @Override
    protected void append(final ILoggingEvent evt) {
        try {
            final URL url = new URL(API_URL);

            final StringWriter w = new StringWriter();
            w.append("token=").append(token).append("&");
            if (channel != null) {
                w.append("channel=").append(URLEncoder.encode(channel, "UTF-8")).append("&");
            }
            if (iconEmoji != null) {
                w.append("icon_emoji=").append(URLEncoder.encode(iconEmoji, "UTF-8")).append("&");
            }
            if (userName != null) {
                w.append("username=").append(URLEncoder.encode(userName, "UTF-8")).append("&");
            }
            if (layout != null) {
                w.append("text=").append(URLEncoder.encode(layout.doLayout(evt), "UTF-8"));
            } else {
                w.append("text=").append(URLEncoder.encode(defaultLayout.doLayout(evt), "UTF-8"));
            }

            final byte[] bytes = w.toString().getBytes("UTF-8");

            CompletableFuture.runAsync(() -> {
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setFixedLengthStreamingMode(bytes.length);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bytes);
                        os.flush();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    addError("Error to post log to Slack.com (" + channel + "): " + evt, e);

                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            addError("Error to post log to Slack.com (" + channel + "): " + evt, ex);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public void setIconEmoji(final String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(final Layout layout) {
        this.layout = layout;
    }

    private Layout defaultLayout = new LayoutBase<ILoggingEvent>() {

        @Override
        public String doLayout(ILoggingEvent event) {
            StringBuffer sbuf = new StringBuffer(128);
            sbuf.append("-- ");
            sbuf.append("[");
            sbuf.append(event.getLevel());
            sbuf.append("]");
            sbuf.append(event.getLoggerName());
            sbuf.append(" - ");
            sbuf.append(event.getFormattedMessage().replaceAll("\n", "\n\t"));
            return sbuf.toString();
        }

    };
}
