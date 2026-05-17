package com.fraudengine.engine;

import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;

public interface Rule {

    RuleResult evaluate(Transaction transaction);
}