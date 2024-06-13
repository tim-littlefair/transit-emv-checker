package net.heretical_camelid.transit_emv_checker.android_app;

import android.content.DialogInterface;

class StartupAlertListener implements DialogInterface.OnClickListener {
    final MainActivity m_mainActivity;

    StartupAlertListener(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                m_mainActivity.m_userHasAgreed = true;
                m_mainActivity.populateAboutPage();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                m_mainActivity.navigateToPage(R.id.navigation_about);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                m_mainActivity.closeApplication();
                break;
        }
    }
}
