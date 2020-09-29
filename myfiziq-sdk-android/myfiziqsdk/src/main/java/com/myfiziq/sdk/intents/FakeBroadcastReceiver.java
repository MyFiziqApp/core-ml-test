package com.myfiziq.sdk.intents;

import android.content.Context;

import com.myfiziq.sdk.enums.IntentPairs;
import com.myfiziq.sdk.lifecycle.ParameterSet;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

/**
 * This BroadcastReceiver does nothing.
 *
 * It's only here to prevent R8 from optimising the "getClass()" method call in
 * {@link MyFiziqBroadcastReceiver#getClassOfGenericT()} into "MyFiziqBroadcastReceiver.class".
 *
 * "getClass()" is supposed to return the name of the class (e.g. "FakeBroadcastReceiver").
 * Not "MyFiziqBroadcastReceiver.class". So this optimisation is wrong.
 *
 * Turning off optimisations in R8 won't disable this optimisation unless we disable R8 completely.
 *
 * See: https://jakewharton.com/r8-optimization-class-reflection-and-forced-inlining/
 * and: https://jakewharton.com/r8-optimization-class-constant-operations/
 *
 * If we ever have a real BroadcastReceiver in the SDK (not the customer app) that extends
 * "MyFiziqBroadcastReceiver" then we won't need this class.
 *
 * The only reason we need this now is because without any classes in the SDK that extend
 * "MyFiziqBroadcastReceiver", R8 will think that the "getClass()" method will always be
 * "MyFiziqBroadcastReceiver.class" and will thus optimise it away...
 * even though it's needed by customer applications that R8 can't see.
 *
 *
 * @hide
 */
// Never delete the @Keep annotation or else R8 will optimise away this class by deleting it and this fix will stop working
@Keep
public class FakeBroadcastReceiver extends MyFiziqBroadcastReceiver<ParameterSet>
{
    public FakeBroadcastReceiver(Context rootContext, IntentPairs intentPairs)
    {
        super(rootContext, intentPairs);
    }

    @Override
    public ParameterSet generateParameterSet(@Nullable ParameterSet parcel)
    {
        return null;
    }
}
