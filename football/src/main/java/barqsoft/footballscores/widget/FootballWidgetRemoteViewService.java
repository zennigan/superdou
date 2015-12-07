package barqsoft.footballscores.widget;

/**
 * Created by Zennigan on 12/7/2015.
 */

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;


import java.text.SimpleDateFormat;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * IntentService which handles updating all Today widgets with the latest data
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class FootballWidgetRemoteViewService extends RemoteViewsService {

    private static final int INDEX_TIME_COL = 2;
    private static final int INDEX_HOME_COL = 3;
    private static final int INDEX_AWAY_COL = 4;
    private static final int INDEX_HOME_GOALS_COL = 5;
    private static final int INDEX_AWAY_GOALS_COL = 6;
    private static final int INDEX_MATCH_ID = 7;

    private static final String[] SCORE_COLUMNS = {
            DatabaseContract.scores_table.LEAGUE_COL,
            DatabaseContract.scores_table.DATE_COL,
            DatabaseContract.scores_table.TIME_COL,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.MATCH_ID,
            DatabaseContract.scores_table.MATCH_DAY,

    };

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                final long identityToken = Binder.clearCallingIdentity();

                // Get today's data from the ContentProvider
                SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");

                Uri scoreUri = DatabaseContract.scores_table.buildScoreWithDate();
                data = getContentResolver().query(scoreUri, SCORE_COLUMNS, null,
                        new String[]{mformat.format(System.currentTimeMillis())}, null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews view = new RemoteViews(getPackageName(),
                        R.layout.widget_football_item);

                String home = data.getString(INDEX_HOME_COL);
                String away = data.getString(INDEX_AWAY_COL);
                String time = data.getString(INDEX_TIME_COL);
                String score = Utilies.getScores(data.getInt(INDEX_HOME_GOALS_COL), data.getInt(INDEX_AWAY_GOALS_COL));

                int home_crest_id = Utilies.getTeamCrestByTeamName(data.getString(INDEX_HOME_COL));
                int away_crest_id = Utilies.getTeamCrestByTeamName(data.getString(INDEX_AWAY_COL));

                view.setTextViewText(R.id.widget_home_name, home);
                view.setTextViewText(R.id.widget_away_name, away);
                view.setTextViewText(R.id.widget_date, time);
                view.setTextViewText(R.id.widget_score, score);
                view.setImageViewResource(R.id.widget_home_crest, home_crest_id);
                view.setImageViewResource(R.id.widget_away_crest, away_crest_id);

                final Intent fillInIntent = new Intent();
                Uri scoreUri = DatabaseContract.scores_table.buildScoreWithDate();
                fillInIntent.setData(scoreUri);
                view.setOnClickFillInIntent(R.id.widget_football_item, fillInIntent);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    //setRemoteContentDescription(views,home_col +" versus " + away_col + ". The score is" +  scores + ". The current time is" + time_col);
                    if(" - ".equals(score)){
                        setRemoteContentDescription(view, String.format(getString(R.string.no_score_board), home, away, time));
                    }else {
                        setRemoteContentDescription(view, String.format(getString(R.string.score_board), home, away, score, time));
                    }
                }

                return view;
            }


            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_football_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_MATCH_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                    views.setContentDescription(R.id.widget_football_item, description);
            }
        };
    }
}
