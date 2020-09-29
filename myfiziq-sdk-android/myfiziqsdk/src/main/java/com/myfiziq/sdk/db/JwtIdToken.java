package com.myfiziq.sdk.db;

import android.text.TextUtils;

import org.joda.time.Instant;

import androidx.annotation.Nullable;

public class JwtIdToken extends Model
{
    @Persistent
    String sub;

    @Persistent(jsonMap = "email_verified")
    boolean emailVerified;

    @Persistent
    String birthdate;

    @Persistent
    String gender;

    @Persistent
    String iss;

    @Persistent(jsonMap = "cognito:username")
    String cognitoUsername;

    @Persistent(jsonMap = "given_name:username")
    String givenName;

    @Persistent
    String aud;

    @Persistent(jsonMap = "event_id")
    String eventId;

    @Persistent(jsonMap = "token_use")
    String tokenUse;

    @Persistent(jsonMap = "auth_time")
    long authTime;

    @Persistent(jsonMap = "custom:join_date")
    String customJoinDate;

    @Persistent(jsonMap = "exp")
    long exp;

    @Persistent(jsonMap = "iat")
    long iat;

    @Persistent(jsonMap = "family_name")
    String familyName;

    @Persistent(jsonMap = "email")
    String email;

    @Persistent(jsonMap = "custom:metric_preferred")
    String customMetricPreferred;

    /**
     * A GUID tat represents the subject being identified.
     */
    public String getSub()
    {
        return sub;
    }

    /**
     * Whether the user has verified their email address.
     */
    public boolean isEmailVerified()
    {
        return emailVerified;
    }

    /**
     * The user's date of birth.
     */
    public String getBirthdate()
    {
        return birthdate;
    }

    /**
     * The user's gender.
     */
    @Nullable
    public Gender getGender()
    {
        if (TextUtils.isEmpty(gender))
        {
            return null;
        }

        String lowercaseGender = gender.toLowerCase();

        if (lowercaseGender.contains("f"))
        {
            return Gender.F;
        }
        else if (lowercaseGender.contains("m"))
        {
            return Gender.M;
        }
        else
        {
            return null;
        }
    }

    /**
     * Identifies the prinicpal that issued the JWT token (usually a URL).
     */
    public String getIss()
    {
        return iss;
    }

    /**
     * Gets the Cognito username.
     */
    // I think this is an auto-incrementing primary key in AWS?
    public String getCognitoUsername()
    {
        return cognitoUsername;
    }

    /**
     * Gets the user's given name.
     */
    public String getGivenName()
    {
        return givenName;
    }

    /**
     * Gets the audience that identifies the recipients that thew JWT is intended for.
     */
    public String getAud()
    {
        return aud;
    }

    /**
     * Gets an ID that represents the authentication event.
     */
    public String getEventId()
    {
        return eventId;
    }

    /**
     * Gets the type of token being used.
     */
    public String getTokenUse()
    {
        return tokenUse;
    }

    /**
     * Gets the Unix timestamp when the authentication event occurred.
     */
    public long getAuthTime()
    {
        return authTime;
    }

    /**
     * Gets the date that the user joined.
     */
    public String getCustomJoinDate()
    {
        return customJoinDate;
    }

    /**
     * Gets the expiration time as a Unix timestamp.
     */
    public long getExp()
    {
        return exp;
    }

    public boolean isExpired()
    {
        long currentUtcTime = Instant.now().getMillis() / 1000L;
        return currentUtcTime > exp;
    }

    /**
     * Get the IAT, the time at which the JWT token was issued.
     */
    public long getIat()
    {
        return iat;
    }

    /**
     * Gets the user's family name.
     */
    public String getFamilyName()
    {
        return familyName;
    }

    /**
     * Gets the user's email address.
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * Gets whether the user prefer's the metric units of measurement.
     */
    public String getCustomMetricPreferred()
    {
        return customMetricPreferred;
    }

}