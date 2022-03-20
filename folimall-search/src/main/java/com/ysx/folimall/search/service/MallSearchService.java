package com.ysx.folimall.search.service;

import com.ysx.folimall.search.vo.SearchParam;
import com.ysx.folimall.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam param);
}
