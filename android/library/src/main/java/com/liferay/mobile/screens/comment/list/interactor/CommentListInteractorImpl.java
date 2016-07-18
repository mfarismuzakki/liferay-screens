package com.liferay.mobile.screens.comment.list.interactor;

import android.support.annotation.NonNull;
import android.util.Pair;
import com.liferay.mobile.android.service.Session;
import com.liferay.mobile.screens.base.list.interactor.BaseListCallback;
import com.liferay.mobile.screens.base.list.interactor.BaseListEvent;
import com.liferay.mobile.screens.base.list.interactor.BaseListInteractor;
import com.liferay.mobile.screens.cache.OfflinePolicy;
import com.liferay.mobile.screens.cache.tablecache.TableCache;
import com.liferay.mobile.screens.comment.list.CommentListListener;
import com.liferay.mobile.screens.models.CommentEntry;
import com.liferay.mobile.screens.service.v70.CommentmanagerjsonwsService;
import com.liferay.mobile.screens.util.LiferayLocale;
import com.liferay.mobile.screens.util.JSONUtil;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

import static com.liferay.mobile.screens.cache.DefaultCachedType.ASSET_LIST;
import static com.liferay.mobile.screens.cache.DefaultCachedType.ASSET_LIST_COUNT;

/**
 * @author Alejandro Hernández
 */
public class CommentListInteractorImpl
	extends BaseListInteractor<CommentEntry, CommentListListener>
	implements CommentListInteractor {

	public CommentListInteractorImpl(int targetScreenletId, OfflinePolicy offlinePolicy) {
		super(targetScreenletId, offlinePolicy);
	}

	@Override public void loadRows(long groupId, String className, long classPK, int startRow, int endRow)
		throws Exception {
		this._groupId = groupId;
		this._className = className;
		this._classPK = classPK;

		Locale locale = LiferayLocale.getDefaultLocale();

		processWithCache(startRow, endRow, locale);
	}

	@NonNull @Override protected CommentEntry getElement(TableCache tableCache) throws JSONException {
		return new CommentEntry(JSONUtil.toMap(new JSONObject(tableCache.getContent())));
	}

	@Override protected String getContent(CommentEntry commentEntry) {
		return new JSONObject(commentEntry.getValues()).toString();
	}

	@Override protected BaseListCallback<CommentEntry> getCallback(Pair<Integer, Integer> rowsRange, Locale locale) {
		return new CommentListCallback(getTargetScreenletId(), rowsRange, locale);
	}

	@Override protected void getPageRowsRequest(Session session, int startRow, int endRow, Locale locale)
		throws Exception {
		CommentmanagerjsonwsService service = getCommentsService(session);
		service.getComments(_groupId, _className, _classPK, startRow, endRow);
	}

	@Override protected void getPageRowCountRequest(Session session) throws Exception {
		CommentmanagerjsonwsService service = getCommentsService(session);
		service.getCommentsCount(_groupId, _className, _classPK);
	}

	@Override protected boolean cached(Object... args) throws Exception {
		final int startRow = (int) args[0];
		final int endRow = (int) args[1];

		return recoverRows(getId(), ASSET_LIST, ASSET_LIST_COUNT, _groupId, null, null, startRow, endRow);
	}

	@Override protected void storeToCache(BaseListEvent event, Object... args) {
		storeRows(getId(), ASSET_LIST, ASSET_LIST_COUNT, _groupId, null, event);
	}

	private CommentmanagerjsonwsService getCommentsService(Session session) {
		return new CommentmanagerjsonwsService(session);
	}

	@NonNull private String getId() {
		return _className + "-" + String.valueOf(_classPK);
	}

	private long _groupId;
	private String _className;
	private long _classPK;
}
