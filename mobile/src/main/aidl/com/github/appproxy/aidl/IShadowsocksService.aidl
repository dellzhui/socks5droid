package com.github.appproxy.aidl;

import com.github.appproxy.aidl.IShadowsocksServiceCallback;

interface IShadowsocksService {
  int getState();
  String getProfileName();

  oneway void registerCallback(IShadowsocksServiceCallback cb);
  oneway void startListeningForBandwidth(IShadowsocksServiceCallback cb);
  oneway void stopListeningForBandwidth(IShadowsocksServiceCallback cb);
  oneway void unregisterCallback(IShadowsocksServiceCallback cb);
}
