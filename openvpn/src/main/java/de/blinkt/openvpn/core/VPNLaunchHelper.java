/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Vector;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;

public class VPNLaunchHelper {
    private static final String MINIPIEVPN = "pie_openvpn";

    private static String writeMiniVPN(Context context) {
        String nativeAPI = NativeUtils.getNativeAPI();
        /* Q does not allow executing binaries written in temp directory anymore */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            File libovpnexec = new File(context.getApplicationInfo().nativeLibraryDir, "libovpnexec.so");
            if (libovpnexec.exists() && libovpnexec.canExecute()) {
                android.util.Log.d("VPNLaunchHelper", "Using libovpnexec.so from native library dir: " + libovpnexec.getPath());
                return libovpnexec.getPath();
            } else {
                android.util.Log.w("VPNLaunchHelper", "libovpnexec.so not found in native library dir (" + libovpnexec.getPath() + "), falling back to pie_openvpn from assets");
                // Fallback to pie_openvpn from assets (same as pre-P behavior)
            }
        }

        String[] abis = Build.SUPPORTED_ABIS;

        if (!nativeAPI.equals(abis[0])) {
            VpnStatus.logWarning(R.string.abi_mismatch, Arrays.toString(abis), nativeAPI);
            abis = new String[]{nativeAPI};
        }

        for (String abi : abis) {

            File vpnExecutable = new File(context.getCacheDir(), "c_" + MINIPIEVPN + "." + abi);
            if ((vpnExecutable.exists() && vpnExecutable.canExecute()) || writeMiniVPNBinary(context, abi, vpnExecutable)) {
                android.util.Log.d("VPNLaunchHelper", "Using pie_openvpn from cache: " + vpnExecutable.getPath());
                return vpnExecutable.getPath();
            }
        }

        throw new RuntimeException("Cannot find any executable for this device's ABIs " + Arrays.toString(abis));
    }


    static String[] buildOpenvpnArgv(Context c) {
        Vector<String> args = new Vector<>();

        String binaryName = writeMiniVPN(c);
        // Add fixed paramenters
        //args.add("/data/data/de.blinkt.openvpn/lib/openvpn");

        args.add(binaryName);

        args.add("--config");
        args.add("stdin");

        return args.toArray(new String[0]);
    }

    private static boolean writeMiniVPNBinary(Context context, String abi, File mvpnout) {
        try {
            InputStream mvpn;

            try {
                mvpn = context.getAssets().open(MINIPIEVPN + "." + abi);
            } catch (IOException errabi) {
                VpnStatus.logInfo("Failed getting assets for architecture " + abi);
                return false;
            }


            FileOutputStream fout = new FileOutputStream(mvpnout);

            byte[] buf = new byte[4096];

            int lenread = mvpn.read(buf);
            while (lenread > 0) {
                fout.write(buf, 0, lenread);
                lenread = mvpn.read(buf);
            }
            fout.close();

            if (!mvpnout.setExecutable(true)) {
                VpnStatus.logError("Failed to make OpenVPN executable");
                return false;
            }


            return true;
        } catch (IOException e) {
            VpnStatus.logException(e);
            return false;
        }

    }


    public static void startOpenVpn(VpnProfile startprofile, Context context, String startReason, boolean replace_running_vpn) {
        android.util.Log.d("VPNLaunchHelper", "startOpenVpn() called for profile: " + startprofile.mName + ", reason: " + startReason);
        Intent startVPN = startprofile.getStartServiceIntent(context, startReason, replace_running_vpn);
        if (startVPN != null) {
            android.util.Log.d("VPNLaunchHelper", "Got start service intent, starting OpenVPNService");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //noinspection NewApi
                android.util.Log.d("VPNLaunchHelper", "Starting foreground service (Android O+)");
                context.startForegroundService(startVPN);
            } else {
                android.util.Log.d("VPNLaunchHelper", "Starting service (pre-Android O)");
                context.startService(startVPN);
            }
            android.util.Log.d("VPNLaunchHelper", "Service start command sent");
        } else {
            android.util.Log.e("VPNLaunchHelper", "getStartServiceIntent() returned null!");
        }
    }
}
