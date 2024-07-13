package com.hmydk.aigit.service.impl;

import com.hmydk.aigit.service.ChangeCollector;
import com.intellij.openapi.vcs.changes.Change;
import com.hmydk.aigit.pojo.ChangeInfo;


import java.util.Collection;
import java.util.List;


/**
 * DefaultChangeCollector
 *
 * @author hmydk
 */
public class DefaultChangeCollector implements ChangeCollector {
    @Override
    public List<ChangeInfo> collectChanges(Collection<Change> changes) {
        return null;
    }
}
