package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/** Debug-only Shizuku UserService exposing one fixed read-only probe transaction. */
public final class RomContextProbeUserService extends Binder {
    static final String DESCRIPTOR = BuildConfig.APPLICATION_ID + ".IRomContextProbe";
    static final int TRANSACTION_CAPTURE = IBinder.FIRST_CALL_TRANSACTION;

    public RomContextProbeUserService() {
        attachInterface(null, DESCRIPTOR);
    }

    public RomContextProbeUserService(Context context) {
        attachInterface(null, DESCRIPTOR);
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        data.enforceInterface(DESCRIPTOR);
        if (code == TRANSACTION_CAPTURE) {
            int target = data.readInt();
            long sinceEpochMs = data.readLong();
            reply.writeNoException();
            try {
                reply.writeString(RomContextProbeCollector.capture(target, sinceEpochMs));
            } catch (Throwable error) {
                reply.writeString("probe-error=" + error.getClass().getSimpleName() + ":"
                        + safeMessage(error));
            }
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    public void destroy() {
        System.exit(0);
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null) return "unknown";
        return message.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
