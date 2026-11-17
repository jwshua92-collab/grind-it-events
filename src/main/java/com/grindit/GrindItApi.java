package com.grindit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to the Grind It site. Every call is asynchronous: the answer (or a
 * short reason it failed) arrives on OkHttp's thread, so callers hop back to
 * the client thread before touching game state.
 */
@Singleton
class GrindItApi
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final MediaType PNG = MediaType.parse("image/png");

	private final OkHttpClient http;
	private final Gson gson;
	private final GrindItConfig config;

	@Inject
	GrindItApi(OkHttpClient http, Gson gson, GrindItConfig config)
	{
		this.http = http;
		this.gson = gson;
		this.config = config;
	}

	boolean hasLinkCode()
	{
		return !linkCode().isEmpty();
	}

	/** GET /api/plugin/state: the live event, the items to watch for, and the password. */
	void fetchState(String rsn, Consumer<EventState> done, Consumer<String> failed)
	{
		HttpUrl url = endpoint("api/plugin/state");
		if (url == null)
		{
			failed.accept("the site address in the plugin settings isn't valid");
			return;
		}
		if (rsn != null)
		{
			url = url.newBuilder().addQueryParameter("rsn", rsn).build();
		}
		call(request(url).get().build(), EventState.class, done, failed);
	}

	/** POST /api/plugin/drop: the drop in Dink's payload shape, with its screenshot. */
	void sendDrop(Map<String, Object> payload, byte[] png, Consumer<DropResult> done, Consumer<String> failed)
	{
		HttpUrl url = endpoint("api/plugin/drop");
		if (url == null)
		{
			failed.accept("the site address in the plugin settings isn't valid");
			return;
		}
		MultipartBody.Builder body = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("payload_json", null, RequestBody.create(JSON, gson.toJson(payload)));
		if (png != null)
		{
			body.addFormDataPart("file", "screenshot.png", RequestBody.create(PNG, png));
		}
		call(request(url).post(body.build()).build(), DropResult.class, done, failed);
	}

	private Request.Builder request(HttpUrl url)
	{
		return new Request.Builder()
			.url(url)
			.header("Authorization", "Bearer " + linkCode())
			.header("X-Grind-It-Plugin", GrindItPlugin.VERSION);
	}

	private String linkCode()
	{
		String code = config.linkCode();
		return code == null ? "" : code.trim();
	}

	private HttpUrl endpoint(String path)
	{
		String base = config.siteUrl();
		if (base == null || base.trim().isEmpty())
		{
			base = GrindItConfig.DEFAULT_SITE;
		}
		HttpUrl root = HttpUrl.parse(base.trim());
		return root == null ? null : root.newBuilder().addPathSegments(path).build();
	}

	private <T> void call(Request request, Class<T> type, Consumer<T> done, Consumer<String> failed)
	{
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				failed.accept("couldn't reach the Grind It site");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					ResponseBody body = r.body();
					String text = body == null ? "" : body.string();
					if (!r.isSuccessful())
					{
						failed.accept(reason(r.code(), text));
						return;
					}
					T value = gson.fromJson(text, type);
					if (value == null)
					{
						failed.accept("the site sent an empty answer");
						return;
					}
					done.accept(value);
				}
				catch (IOException | JsonParseException e)
				{
					failed.accept("couldn't read the site's answer");
				}
			}
		});
	}

	/** The site's own reason ({"detail": "..."}) where it gives one. */
	private String reason(int code, String text)
	{
		try
		{
			JsonObject o = gson.fromJson(text, JsonObject.class);
			if (o != null && o.has("detail") && o.get("detail").isJsonPrimitive())
			{
				return o.get("detail").getAsString();
			}
		}
		catch (JsonParseException e)
		{
			// not JSON: fall through
		}
		if (code == 401)
		{
			return "your link code isn't valid: get a new one from the plugin page";
		}
		return "the site answered " + code;
	}
}
