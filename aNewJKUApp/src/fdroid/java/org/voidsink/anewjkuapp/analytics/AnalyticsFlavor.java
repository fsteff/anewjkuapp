/*
 *       ____.____  __.____ ___     _____
 *      |    |    |/ _|    |   \   /  _  \ ______ ______
 *      |    |      < |    |   /  /  /_\  \\____ \\____ \
 *  /\__|    |    |  \|    |  /  /    |    \  |_> >  |_> >
 *  \________|____|__ \______/   \____|__  /   __/|   __/
 *                   \/                  \/|__|   |__|
 *
 *  Copyright (c) 2014-2019 Paul "Marunjar" Pretsch
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.voidsink.anewjkuapp.analytics;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AnalyticsFlavor implements IAnalytics {


    private static final Logger logger = LoggerFactory.getLogger(AnalyticsFlavor.class);

    @Override
    public void init(Application app) {
        logger.debug("init");
    }

    @Override
    public void sendException(Context c, Exception e, boolean fatal, List<String> additionalData) {
        if (e != null) {
            logger.debug(e.getMessage());
            if (additionalData != null) {
                for (String value : additionalData) {
                    if (!TextUtils.isEmpty(value)) {
                        logger.debug("additional info: {}", value.substring(0, Math.min(value.length(), 4096)));
                    }
                }
            }
        }
    }

    @Override
    public void sendScreen(Activity activity, String screenName) {
        logger.debug("screen: {}", screenName);
    }

    @Override
    public void sendButtonEvent(String label) {
        logger.debug("buttonEvent: {}", label);
    }

    @Override
    public void sendPreferenceChanged(String key, String value) {
        if (!TextUtils.isEmpty(key)) {
            logger.debug("preferenceChanged: {}={}", key, value);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        logger.debug("setEnabled: {}", enabled);
    }

    @Override
    public PlayServiceStatus getPsStatus() {
        return PlayServiceStatus.PS_NOT_AVAILABLE;
    }
}
