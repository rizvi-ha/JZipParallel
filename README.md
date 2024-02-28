# JZipParallel

This was a project to do pigz-style parallelized gzip compression but using Java (pigz is a parallelized streaming version of gzip written in C).

It was great for learning more about how concurrency works in Java and getting to work with low-level optimization challenges.

## Benchmarks

pigz by default does compression level 6, so my JZipParallel also is doing compression level 6

For brevity I am only going to list the Real time for each benchmark. The patterns observed for Real time tended to apply 
to user and sys time as well, except that those were usually a bit higher due to having multiple threads. Time is in seconds.
    
The results from 3 trials and their average shown for each.

This is using the number of available processors as our number of workers, as this is default behavior of pigz and JZParallel.

  COMMANDS USED:

        time gzip </usr/local/cs/jdk-21.0.2/lib/modules >gzip.gz
        time pigz </usr/local/cs/jdk-21.0.2/lib/modules >pigz.gz
        time java JZipParallel </usr/local/cs/jdk-21.0.2/lib/modules >JZipParallel.gz
        ls -l gzip.gz pigz.gz JZipParallel.gz

  REAL TIME RESULTS (1, 2, 3 | avg):
  
        gzip - 8.293, 8.411, 8.278 | 8.327
        pigz - 3.152, 2.526, 2.615 | 2.764
        JZP  - 3.227, 3.186, 3.140 | 3.184

  COMPRESSION RATIO:
  
        gzip - 0.3383
        pigz - 0.3376
        JZP  - 0.3376

As you can see my program was on-par with pigz, being a little bit slower. This little bit of extra performance that pigz has
over my Java implementation is likely due to C being a little faster in general, and also the people programming pigz being a lot
more experienced than me.

Nonetheless, it is exciting to see the potential of Java for multithreaded applications and I learned a lot doing this project.

 
