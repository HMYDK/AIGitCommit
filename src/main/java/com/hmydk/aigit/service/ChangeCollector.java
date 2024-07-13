package com.hmydk.aigit.service;

import com.hmydk.aigit.pojo.ChangeInfo;
import com.intellij.openapi.vcs.changes.Change;


import java.util.Collection;
import java.util.List;

/**
 * ChangeCollector
 *
 * @author hmydk
 */
public interface ChangeCollector {
    List<ChangeInfo> collectChanges(Collection<Change> changes);

}
