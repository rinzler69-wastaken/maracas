package id.josjis.maracas

import android.app.admin.DeviceAdminReceiver

/** Device-admin component whose only policy (res/xml/device_admin.xml) is force-lock, used by "Turn screen off". */
class MaracasAdminReceiver : DeviceAdminReceiver()
