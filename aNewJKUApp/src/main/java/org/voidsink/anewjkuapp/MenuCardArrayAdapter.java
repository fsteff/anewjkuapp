package org.voidsink.anewjkuapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.voidsink.anewjkuapp.calendar.CalendarCard;
import org.voidsink.anewjkuapp.view.DateHeaderCard;
import org.voidsink.anewjkuapp.view.ExamCardListView;
import org.voidsink.anewjkuapp.view.MenuCardListView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardView;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by paul on 06.09.2014.
 */
public class MenuCardArrayAdapter extends CardArrayAdapter implements StickyListHeadersAdapter {

    protected MenuCardListView mMenuListView;

    /**
     * Constructor
     *
     * @param context The current context.
     * @param cards   The cards to represent in the ListView.
     */
    public MenuCardArrayAdapter(Context context, List<Card> cards) {
        super(context, cards);
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup viewGroup) {

        // Build your custom HeaderView
        //In this case I will use a Card, but you can use any view

        LayoutInflater mInflater = LayoutInflater.from(getContext());
        View view = mInflater.inflate(R.layout.menu_card_header, null);

        CardView cardView= (CardView)view.findViewById(R.id.menu_card_header_id);
        Card card = getItem(position);
        if (card instanceof MenuCard) {
            Card headerCard = new DateHeaderCard(getContext(), ((MenuCard) card).getDay().getDate());
            cardView.setCard(headerCard);
        }
        return view;
    }

    @Override
    public long getHeaderId(int position) {
        Card card = getItem(position);
        if (card instanceof MenuCard) {

            Calendar cal = Calendar.getInstance(); // locale-specific
            cal.setTimeInMillis(((MenuCard) card).getDay().getDate().getTime());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }
        return 0;
    }

    public void setMenuListView(MenuCardListView menuCardListView) {
        this.mMenuListView = menuCardListView;
    }}
