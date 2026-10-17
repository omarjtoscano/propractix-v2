package com.propractix.academicinstitution.application.port.out;

import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;

@FunctionalInterface
public interface AcademicInstitutionIdGenerator {

    AcademicInstitutionId nextId();
}
