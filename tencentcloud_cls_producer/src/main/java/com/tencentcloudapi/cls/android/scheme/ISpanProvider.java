package com.tencentcloudapi.cls.android.scheme;

import java.util.List;

public interface ISpanProvider {
    Resource provideResource();

    List<Attribute> provideAttribute();
}
