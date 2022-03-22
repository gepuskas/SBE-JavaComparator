# SBE-JavaComparator
The purpose of this tool is to analyze a group of two different java modules, specifically the java files, in a directory
The current use case is to compare "*-hbase modules" with "*-bigtable" ones in Persistence project in SBE

https://github.com/SymphonyOSF/SBE/

The conversion from Hbase 0.98 to BigTable (Hbase 1.4.X) code causes some classes to change even though they are equivalent:
HTableInterface -> Table
HConnection -> Connection
...

These conversion rules are currently hardcoded and make sure that in case of code conversion, that is not counted as a DIFF

Apart from this, other cases could arise where a whole block of logic differs between the two modules, e.g

        AESWrappedContentKey key = mapResult(result);
        LOG.info("execFuzzyRowFilterScan retrieved " + key.toString() + " items" );
        keys.add(key);
        
        vs
        
        keys.add(mapResult(result));
        
The solution to ignore this DIFF is to surround the code like this

        //*COMPARATOR-IGNORE-START
        AESWrappedContentKey key = mapResult(result);
        LOG.info("execFuzzyRowFilterScan retrieved " + key.toString() + " items" );
        keys.add(key);
        //*COMPARATOR-IGNORE-STOP
        
in both of the modules
