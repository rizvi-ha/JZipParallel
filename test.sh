if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <filename>"
    exit 1
fi

# Compress using JZipParallel and then decompress using pigz -d and make sure the cmp output is empty
javac JZipParallel.java;
java JZipParallel <$1 >compressed.gz;
pigz -d <compressed.gz >decompressed;
cmp decompressed $1;
