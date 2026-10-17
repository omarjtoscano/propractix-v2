package com.propractix.academicinstitution.application.port.in;

import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;

public interface SearchPublishedInstitutions {

    SearchPublishedInstitutionsResult search(SearchPublishedInstitutionsQuery query);
}
