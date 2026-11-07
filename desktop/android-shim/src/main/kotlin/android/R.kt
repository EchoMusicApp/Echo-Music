package android

// Desktop stand-in for Android's framework resource ids (only the ones the app uses). Negative ids are looked up in stringResource().
object R {
    object string {
        const val cancel = -1
        const val ok = -2
    }
    object drawable {
        const val ic_menu_close_clear_cancel = -10
        const val stat_sys_download = -11
        const val stat_sys_download_done = -12
        const val stat_sys_warning = -13
    }
}
