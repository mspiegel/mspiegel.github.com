---
layout: default
title: Michael Spiegel
---

Michael Spiegel, Ph.D.  
Master Software Engineer, Capital One  
Twitter: [@DrMajorMcCheese](http://twitter.com/DrMajorMcCheese)  
Career: [Resume](/resume-mspiegel.pdf)  
Brain Dump: [Techno Fomo](/technofomo)  
Publications: [Google Scholar](http://scholar.google.com/citations?user=eezjo4sAAAAJ)  

## Computer Science Canon

In studying computer science, I always wished for more emphasis on the
history of the field. I like to read these formative papers in order to (a)
understand the intellectual development of computer science, and (b) inspire
new avenues of thinking in future research. Here is my [Great Works in Computer
Science](/canon) reading list.  Please suggest any additions to the list.

## Open Source Projects

### Checks-Out

[Checks-Out](https://github.com/capitalone/checks-out) is a simple pull request
approval system using GitHub protected branches and maintainers files.
Pull requests are locked and cannot be merged until the minimum number of approvals
are received. Project maintainers can indicate their approval by commenting on the
pull request and including "I approve" in their approval text. Checks-Out also
provides integration with GitHub Reviews. An accepted GitHub Review is counted
as an approval. GitHub Review that requests additional changes blocks the pull
request from merging.

Some significant features in Checks-Out that are not (yet) in GitHub Reviews are:
custom approval policies, different approval policies for different branches and/or
file paths, optional auto-merge when all status checks have passed, optional
auto-tagging of merges.

### Hydra

[Hydra](http://github.com/addthis/hydra) is a distributed data processing and
storage system originally developed at AddThis. It ingests streams of data
(think log files) and builds trees that are aggregates, summaries, or
transformations of the data. These trees can be used by humans to explore (tiny
queries), as part of a machine learning pipeline (big queries), or to support
live consoles on websites (lots of queries). Hydra is most similar in feature
set to Google Dremel / Apache Drill and Apache Samza.

### ssync

[ssync](http://github.com/addthis/ssync) is a divide-and-conquer file copying
tool to multiple destination hosts. It transfers to N remote machines in log N
iterations. ssync is a thin transparent layer on top of rsync that accepts
nearly all the command-line options available to rsync.

### stream-lib

[stream-lib](http://github.com/addthis/stream-lib) is a Java library for
summarizing data in streams for which it is infeasible to store all events.
More specifically, there are classes for estimating: cardinality (i.e. counting
things); set membership; top-k elements and frequency.


## Academia

### Hierarchical work-stealing

The high performance computing group at [RENCI](http://renci.org/) is
developing an efficient hierarchical OpenMP implementation for the open-source
[Qthreads](http://www.cs.sandia.gov/qthreads) runtime library. Efficient
scheduling of tasks on modern multi-socket many-core shared memory systems
requires consideration of an increasingly complex memory hierarchy. In a
traditional work-stealing framework, each core is assigned a double-ended queue
(a dequeue). One side of the dequeue supports sequential operations while the
other side supports concurrent modification. In contrast, our runtime assigns
several cores to a single dequeue. For these cores on the same chip, the LIFO
task scheduling allows exploitation of cache locality between sibling tasks as
well as between a parent task and its newly created child tasks. My contribution
is the design and implementation of an array-based lock-free dequeue that
supports concurrent modification on both ends of the data structure.

The paper "OpenMP Task Scheduling Strategies for Multicore NUMA Systems" has
been accepted for publication in the peer-reviewed journal _International
Jounral of High Performance Computing Applications_.

### Convergent Haplotype Association Tagging

A haplotype is a DNA sequence that has been inherited from one parent. Each
person possesses two haplotypes for most regions of the genome. The process of
deducing haplotype information based on genotype data is known as _haplotype
phasing_. Haplotype information is important in untangling the heritability of
traits of interest such as generic disorders. We can use genetic markers that
are uncommon in the population as beacons to identify common ancestors for
individuals that are not known to be related. The design of this phasing
strategy is ongoing. My responsibilities involve scaling our phasing algorithm
to thousands of individuals and hundreds of thousands of genetic markers per
individual.

The [OpenMx Project](http://openmx.psyc.virginia.edu) intends to rewrite and
extend the popular statistical package Mx to address the challenges facing a
large range of modern statistical problems such as: (i) the difficulty of
measuring behavioral traits; (ii) the availability of technologies - such as
such as magnetic resonance imaging, continuous physiological monitoring and
microarrays - which generate extremely large amounts of data often with complex
time-dependent patterning; (iii) increased sophistication in the statistical
models used to analyze the data. To address these problems, the Mx Structural
Equation Modeling software will be rewritten so as to: (i) split OpenMx into
modules that interoperate with the R statistical package; (ii) release OpenMx as
open source so as to provide a stable path for future maintenance and
development; (iii) integrate OpenMx with the Swift (formerly VDL) parallel
workflow software.

The paper "OpenMx: An Open Source Extended Structural Equation Modeling
Framework" has been accepted for publication in the peer-reviewed journal
_Psychometrika_, the print journal of the Psychometric Society.

### Cache-conscious concurrent data structures

The power wall, the ILP wall, and the memory wall are driving a trend from
implicitly parallel architectures towards explicitly parallel architectures.
The memory wall has been identified as one of the fundamental challenges to
high-performance concurrent computing. The design of cache-conscious concurrent
data structures for many-core systems will show significant performance
improvements over the state of the art in concurrent data structure designs for
those applications that must contend with the deleterious effects of the memory
wall. The design of cache-conscious, linearizable concurrent data structures
has advantageous properties that can be measured across multiple architecture
platforms. My dissertation research fills the gap in cache-conscious concurrent
data structures by providing concurrent algorithms that implement an ordered
set abstract data type. The dense skip tree is a randomized data structure that
has been designed to probabilistically exploit spatial locality of reference.
The dense skip tree causes fewer cache misses than self-balancing binary search
trees by probabilistically aggregating consecutive sequences of keys into
contiguous regions of memory. The primary contributions of my thesis are the
optimistic skip tree algorithm, the lock-free concurrent skip tree algorithm,
and the lock-free concurrent HAT trie algorithm.

I successfully defended my dissertation on April 18, 2011. A copy of the thesis
is [available online](publications/michael-spiegel-dissertation.pdf).

### Fortress

[Fortress](https://en.wikipedia.org/wiki/Fortress_(programming_language)) was a
programming language designed for high-performance computing with high
programmability. Fortress will support features such as transactions,
specification of locality, and implicit parallel computation as integral
features built into the core of the language. Features such as the Fortress
component system and test framework facilitate program assembly and testing, and
enable powerful compiler optimizations across library boundaries. The syntax and
type system of Fortress are custom-tailored to modern HPC programming,
supporting mathematical notation and static checking of properties such as
physical units and dimensions, static type checking of multidimensional arrays
and matrices, and definitions of domain-specific language syntax in libraries.
