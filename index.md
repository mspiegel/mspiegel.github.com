---
layout: default
title: Michael Spiegel
---

Michael Spiegel, Ph.D.  
Twitter: ~~@DrMajorMcCheese~~ (deleted)  
Career: [Resume](https://www.linkedin.com/in/michael-spiegel-908a3627)  
Open Source: [GitHub Profile](https://github.com/mspiegel)  
Publications:
[Google Scholar](https://scholar.google.com/citations?user=eezjo4sAAAAJ)  
Brain Dump: [Techno Fomo](/technofomo)  

## Computer Science Canon

In studying computer science, I always wished for more emphasis on the
history of the field. I like to read these formative papers in order to (a)
understand the intellectual development of computer science, and (b) inspire
new avenues of thinking in future research. Here is my [Great Works in Computer
Science](/canon) reading list. Please suggest any additions to the list.

## Recent Projects

### The Mesh

Core contributor to [the Mesh](https://www.hushmesh.com), a Rust-based
confidential-computing platform, with deep ownership across the system's
foundational subsystems. Designed and implemented
the platform's trust and key-management substrate — including remote attestation
for SGX, TDX, and Azure vTPM environments; bootstrap, recovery, rotation, and
disaster-recovery flows for the distributed key-management services; mesh-wide
root and intermediate certificate rotation; and end-to-end PKI with hybrid
post-quantum (Kyber) key exchange and ECDSA/RSA signing. Built and evolved major
server and listener subsystems from the ground up, including a storage service
with Azure-backed encrypted disk management, real-time collaboration features
with WebRTC/STUN/TURN signaling and group messaging, and the certificate
management service. Led the HTTP/2 listener replacement for the legacy HTTPS
listener, including a port of `hyperium/h2`, h2spec-compliant frame and stream
state machines, header validation, and real flow control. Drove cross-cutting
platform work — verifiable credentials (BBS+, JSON-LD, ECDSA-RDFC-2019),
saga/outbox patterns, async handler conversions, a workspace-wide Clippy
rollout, secret-handling and logging discipline, and integration-test
infrastructure — while routinely diagnosing and fixing deep bugs in attestation,
deadlocks, certificate bookkeeping, and race conditions across the distributed
system.

### OEIS A395493

[A395493](https://oeis.org/A395493) enumerates the tilings of an *n* × *n*
square by exactly *n* fixed polyominoes — equivalently, the partitions of the
cells of an *n* × *n* grid into exactly *n* orthogonally-connected regions.
I contributed the sequence and its first eleven terms in April 2026, ranging
from a(1) = 1 and a(2) = 6 to a(11) ≈ 6.9 × 10^38.

The terms are computed by a Rust implementation of a frontier dynamic program,
a technique with an established history. Knuth's Simpath (TAOCP 4B §7.1.4) is
its ancestor; Harris applied a row-by-row slice-state recurrence of this kind to
[polyomino tilings of the n × n square](http://www.bumblebeagle.org/polyominoes/tilingcounting/counting_9x9_tilings.pdf)
in 2010; and the zero-suppressed decision diagram literature developed the
graph-partition case, where the algorithm of
[Kawahara, Horiyama, Hotta and Minato](https://link.springer.com/chapter/10.1007/978-3-319-53925-6_10)
(WALCOM 2017) underlies Graphillion's partition specifications. The program
sweeps the board in row-major order, maintaining a sliding window of region
labels in restricted-growth canonical form so that equivalent partial states
are identified. The principal difficulty is over-counting: opening a new region
at a cell and merging it into a neighbor at a later step yields the same
partition as joining that neighbor immediately, and a naive formulation tallies
both. Following the edge-decision rule of Kawahara et al., as implemented in
Graphillion's `GraphRangePartitionSpec`, each cell placement instead commits
its two incident grid edges as taken or not taken, and every not-taken edge
records the corresponding pair of regions in a Forbidden-Pair Set that
prohibits any subsequent merge. The result is a bijection between partitions
and edge-decision sequences, under which each tiling is counted exactly once.
The specialization to this sequence is a count of completed regions carried in
the state, constraining the partition to exactly *n* blocks of unrestricted
size; Harris instead tracks the area of each partial region, which suits a
fixed piece size but cannot express a fixed block count.

Memory, rather than time, is the binding constraint on how far the sequence can
be extended: each additional term multiplies the size of the frontier layer,
and the computation aborts long before it becomes slow. The optimization effort
was directed accordingly. The frontier state is packed into two `u128` words —
32 bytes covering fifteen 5-bit label slots, a closed-region counter, and the
120-bit forbidden-pair triangle — and each layer is aggregated by sorting and
folding a flat `Vec` rather than by random-access hash map updates, which
eliminates roughly 24 bytes of per-entry overhead. Horizontal mirror symmetry
collapses each state with its reflection at row boundaries — an identification
Harris also employs — and a reachability bound prunes states that can no longer
resolve to exactly *n* regions. The allocator proved to matter as much as the
data structures: mimalloc retained freed pages rather than returning them to the
operating system, holding some twelve times the peak static layer size, and
reverting to the system allocator halved peak resident memory at a cost of five
percent in wall time. In combination these measures reduced n = 10 from 9.4 GB
of resident memory to 1.9 GB, and from 86 seconds to 29 seconds.

The results are validated against three independent implementations: a
brute-force enumerator over all labelings, an exhaustive set-partition
enumerator with a connectivity test, and a ZDD-based count using the
Graphillion library, the last of which is tractable through n = 7 before
exhausting 32 GB of memory.

### OEIS A142886

[A142886](https://oeis.org/A142886) counts the polyominoes of *n* cells whose
own symmetry group is the full dihedral group of the square — the eight-element
group of rotations and reflections, and the largest symmetry group a polyomino
can possess. The sequence was contributed by N. J. A. Sloane in 2009. I extended
its b-file from *n* = 163 to *n* = 203, the earlier terms being due to Robert A.
Russell; the [Rust program](https://github.com/mspiegel/A142886) that produced
the new terms is linked from the entry. All terms vanish unless *n* ≡ 0 or 1
(mod 4), a consequence of orbit arithmetic: the group partitions a symmetric
polyomino's cells into orbits of size one, four, or eight.

The enumeration never constructs a full polyomino. A shape fixed by the entire
group is determined by its intersection with a fundamental domain — the 45°
wedge 0 ≤ y ≤ x — because the remaining seven octants are then forced, so the
program enumerates only wedge slices and weights each cell by the size of its
orbit. Two cases arise, according to whether the center of symmetry falls at
the center of a cell or at a lattice vertex; no other placement is compatible
with a quarter turn, and the two enumerations are disjoint because a shape's
centroid coincides with its symmetry center. Within a wedge the slices are
generated by Redelmeier's recursive cell growth, which visits each connected
shape exactly once and therefore requires no deduplication pass.

Connectivity of the full polyomino is decided locally, on the roughly *n*/8
cells of the slice: the shape is connected precisely when the slice is itself
connected and touches both wedge edges, since the eight octant copies are glued
to one another along exactly those edges. This criterion admits a strong
pruning rule. Until the slice reaches the diagonal edge, the program carries a
lower bound on the additional orbit weight required to reach it, and abandons
any branch whose current weight plus that bound already exceeds *n*.

The remaining work was constant-factor. Slices are bucketed by their minimal
x-axis cell, which serves as a canonical representative and renders the buckets
independent search problems; the residue of *n* modulo four determines which
buckets can contribute at all, and the others are skipped without recursion.
The three per-cell predicates of the growth loop — in-slice, blocked, and
queued — are folded into a single byte-per-cell state machine over a contiguous
array, which together with related representational changes reduced single-core
time by approximately 31%. Buckets are then distributed across cores with
Rayon, with further fan-out at bounded recursion depth so that a few large
buckets cannot bottleneck a many-core machine, for a speedup of roughly 7.9× at
*n* = 104. Every optimization is count-preserving: enabled or disabled, the
output is byte-identical over the verified range, and the program reproduces
all 164 previously published terms exactly. The final run took approximately
seven days, appending each term to a checkpoint file as it completed so that an
interruption would cost at most the term in progress.

### OEIS A157418

[A157418](https://oeis.org/A157418) arises in the theory of Rota-Baxter
algebras. A Rota-Baxter operator is a linear map `P` satisfying
`P(u)P(v) = P(uP(v)) + P(P(u)v)`, which is the rule of integration by parts
written without integral signs: taking `P` to be integration from zero
recovers the familiar identity exactly. Free Rota-Baxter algebras have a basis
of *Rota-Baxter words* — expressions built from a generator `x` and the
operator, with `P` written as a pair of parentheses — and the counting of these
words was studied by Guo and Sit, who contributed this sequence in 2010. In
June 2026 I contributed a formula to the entry, found in collaboration with
Claude, expressing the sequence in terms of the Catalan numbers.

The relations reduce every expression to a normal form: the Rota-Baxter
identity rewrites any product of two parenthesized terms, the operator is
idempotent, and the generator is idempotent. What survives is a strictly
alternating sequence of single `x`s and parenthesized blocks, with no pair of
parentheses immediately enclosing another. A157418 counts the *associate*
words, those beginning or ending in `x`: for `n = 3` they are `((x)x)x`,
`(x(x))x`, `x((x)x)`, `x(x)x`, and `x(x(x))`.

The Catalan numbers `1, 1, 2, 5, 14, 42, 132, …` count the objects that arise
whenever a structure is built by repeatedly splitting something in two: the
balanced strings of *n* pairs of parentheses, the binary trees with *n* nodes,
the triangulations of a polygon. Their generating function `F` satisfies
`F(t) = 1 + t·F(t)^2`, the algebraic form of that single splitting rule.

Rota-Baxter words are not Catalan objects — their counts, `1, 2, 5, 16, 55,
202`, follow the Catalan numbers for three terms and then diverge, 16 against
14 — but they turn out to be Catalan objects in disguise. Guo and Sit showed as
much for the count of *all* Rota-Baxter words, whose generating function they
wrote as `(1 + t)·F(t + t^2)`, while leaving the associate case in raw
algebraic form. Writing `B(t) = F(t + t^2)`, the generating function of
A157418 is `t·B(t)·(1 + t·B(t))`, which is equivalent to the algebraic
generating function already recorded in the entry. Substituting `t + t^2`
for `t` leaves the recursive splitting structure untouched and only changes
what an atom weighs: each atom of the Catalan object now carries one object or
two rather than exactly one. The outer factor is what absorbs the condition
that a word begin or end in `x`: it says that a word is one such structure, or
an ordered pair of them.
