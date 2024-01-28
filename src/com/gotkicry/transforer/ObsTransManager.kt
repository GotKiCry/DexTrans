package com.gotkicry.transforer

import com.gotkicry.transforer.bean.ObsTransClass
import com.gotkicry.transforer.obs.ObsFactor

class ObsTransManager {

    companion object{
        private lateinit var obsTransClassMap : Map<String, ObsTransClass>

        fun initMapping(mappingFilePath: String) {
            obsTransClassMap = ObsFactor().parseMapping(mappingFilePath)
        }

        fun getObsTransClass(className: String): ObsTransClass? {
            return obsTransClassMap[className]
        }


    }
}