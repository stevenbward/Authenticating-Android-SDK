[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
[![API](https://img.shields.io/badge/API-14%2B-green.svg?style=flat)](https://android-arsenal.com/api?level=14)
[![JitPack](https://jitpack.io/v/stevenbward/Authenticating-Android-SDK.svg)](https://jitpack.io/#stevenbward/Authenticating-Android-SDK)
[![Build Status](https://travis-ci.org/stevenbward/Authenticating-Android-SDK.svg?branch=master)](https://travis-ci.org/stevenbward/Authenticating-Android-SDK)


# Introduction
Authenticating is a verification platform that helps verify individuals through a variety of tests; the SDK allows companies to develop costom applications that utilize these various tests. Currently we offer solutions for iOS and Android platforms via an SDK as well as an open Api.

# Link To Full Documentation:
<a href='https://docs.authenticating.com/'>Full Documentation Can Be Found Here</a>

# Android Authenticating Quickstart Guide
This is the code for the Authenticating SDK. The examples section is in a different respository. 

## Requirements

<aside class="notice">Note that this requires API Level 14 or higher</aside><br>

## Installation
```java

//Step 1
allprojects {
    repositories {
		maven { url 'https://jitpack.io' }
    }
}

//Step 2
android {
    dependencies {
		compile 'com.github.stevenbward:Authenticating-Android-SDK:2.0.0'
		compile 'com.squareup.retrofit2:retrofit:2.3.0'
		compile 'com.squareup.okhttp3:okhttp:3.9.0'
		compile 'com.squareup.okhttp3:logging-interceptor:3.9.0'
		compile 'com.squareup.retrofit2:converter-gson:2.3.0'
		compile 'com.google.code.gson:gson:2.8.1'
		compile 'com.squareup.okio:okio:1.13.0'
    }
}

//Step 3 (In the Manifest Application Tag)
<application
	.
	.
	android:largeHeap="true"
	>
	
```

It is likely you will need Multi-dex enabled if you have a large number of additional libraries enabled as this includes a few dependencies of its own.

As this SDK involves uploading images (2 at a time), you will likely see OutOfMemory exceptions thrown unless you include the third line regarding largeHeap as true in your manifest file. 

The Authenticating android SDK can be installed via gradle as well is using an aar file (sent upon request). Once successfully integrated, the SDK is ready to go without any custom configuration needed. See the Important Notes section for more information regarding customizing details. For more information, please feel free to Contact us:

 info@authenticating.com

 Toll Free (833) 2-VERIFY
 
 9350 Wilshire Blvd, Suite 203, Beverly Hills, CA 90212
 


## Important Notes

A few important details that apply to all using the SDK:

* The authToken used in the calls is the one received by signing up for an account. Once you have been approved, it will be sent to you with information regarding pricing. This API key is unique and should not be shared to others. Furthermore, it is recommended that this api key be persisted in a safe location. For android, as reverse engineering is possible, it is recommended to store this via your work server and retrieve it via a call of your choosing.


* Also with regards to the User's accessCode, it is linked directly to the companyId. If you have multiple users, their access codes will be unique (while in active state) and match to the companyId. If you use our service for more than one company, you will need to use multiple companyId variables to differentiate between the two. 


### API Calls
All api calls can be found in the <b>AuthenticatingAPICalls</b> class. This class contains static methods which serve as a wrapper to access the API. Each of the methods contain a counterpart/ partner overloaded method that differs in that it takes in or exludes the OnTaskComplete interface listener.

If you call the method that uses the OnTaskComplete listener, the following happens:

1. The call will be made asynchronously and will not require an AsyncTask/ thread wrapper.

2. The method will handle the AuthenticatingError objects that can be thrown and pass them back via the listener if they occur.

3. The method will take care of object casting and pass back the respective object with an integer tag to match the type (More on this in a moment).

If you instead want to handle the running on your own thread, do not want to use the listener, or want to get response objects back without checking for the int type, feel free to use the overloaded / mirrored methods (of the same name) which do not take in the OnTaskComplete listener as an argument.

TL;DR / Summary, every method in the AuthenticatingAPICalls will have an overloaded option that takes in a listener. If you use the one with the listener, the response will be void and is intead sent back on the interface link. If you use the one without the listener, the response is instant and requires thread management.


### Logging JSON Responses

```java
WebCallsLogging.setJsonLogging(true);
```

If you want to view the responses from the server in your logcat for debugging purposes, simply type the code to the right anywhere and it will set the logging to true until you specify false or remove the set to true code.
 
### Nested Libraries

This library utilizes the following Dependencies:

* <a href='https://github.com/square/retrofit'>Retrofit 2.3.0</a>

* <a href='https://github.com/square/okhttp'>Okhttp 3.8.1</a>

* <a href='https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor'>OkHttp Logging 3.8.1</a>

* <a href='https://github.com/square/retrofit/tree/master/retrofit-converters/gson'>Gson Converter 2.3.0 (Via Retrofit)</a>

* <a href='https://github.com/google/gson'>Gson 2.8.1</a>


### Constant Integer Tags
If you choose to use the API Calls that use the OnTaskComplete listener, they will send back an integer tag that defines the return type so you don't need to run if checks on instanceof. The int tags it uses are located in the Constants class under the misc package. I have listed the most used ones here:

* public static final int	TAG_SIMPLE_RESPONSE 19000

* public static final int	TAG_ERROR_RESPONSE 19001

* public static final int	TAG_AVAILABLE_NETWORKS 19002

* public static final int	TAG_USER 19003

* public static final int	TAG_QUIZ_QUESTIONS 19004

* public static final int	TAG_CHECK_PHOTO_RESULT 19005


