# Rough AWS capacity and cost estimate

> A back-of-the-envelope estimate, made on 2026-10-05, of how many instances and how much money the service would need at different play rates. It's directional only: the CPU cost per play is an assumption, not a measurement, and prices change. An earlier version said 2,000 plays a second per vCPU "keeping CPU use around 60–70%", but at 500 µs a play that's 100% of a vCPU; it was corrected to about 1,300, which raised the instance counts about 1.5×. Its purpose is to put the colour representation investigation (`investigations/README.md`) in context.

## Assumptions

- **CPU per play: about 500 µs.** The game logic itself takes about 0.1–1 µs a play (measured in `investigations/`). The rest is the service's own work: Tomcat, Spring MVC, Jackson JSON, and the database client making two network calls. Keeping CPU use at about 65% to leave headroom, that's about 1,300 plays a second per vCPU (0.65 ÷ 0.5 ms). A load test against the real service would replace this guess. See [How sensitive this is](#how-sensitive-this-is) for much less generous figures.
- **Instance:** c7g.large (2 vCPUs, about 2,600 plays a second), $0.0725/h on demand, about $53 a month. At least 2 instances, in two availability zones, so one failing doesn't take the service down.
- **State:** with more than one instance, the machines' floats live in a shared store (see the decision log). Each play reads the float and conditionally writes it back (optimistic locking). A machine's config never changes, so each instance can cache it.
- **Traffic is steady**, in us-east-1, at on-demand prices. Real traffic is peaky, so in practice you'd autoscale.

A month is taken as 730 hours (2,628,000 seconds).

## Compute

| Plays a second | Plays a day | Instances | Compute a month |
|---|---|---|---|
| 100 | 8.6 million | 2 (minimum) | ~$105 |
| 1,000 | 86 million | 2 (minimum) | ~$105 |
| 10,000 | 864 million | 4 | ~$210 |
| 100,000 | 8.6 billion | 39 | ~$2,100 |
| 1,000,000 | 86 billion | 385 | ~$20,000 |

## Storing the floats

| Plays a second | DynamoDB on-demand | DynamoDB provisioned | Valkey (in memory) |
|---|---|---|---|
| 100 | ~$390 | ~$80 | ~$200 |
| 1,000 | ~$3,900 | ~$800 | ~$200 |
| 10,000 | ~$39,000 | ~$8,100 | ~$200 |
| 100,000 | ~$390,000 | ~$81,000 | ~$400 |
| 1,000,000 | ~$3.9 million | ~$810,000 | ~$4,100 |

- **DynamoDB on-demand:** one strongly consistent read and one write under 1 KB per play, at $0.25 per million reads and $1.25 per million writes. Sources disagreed on whether these already include the November 2024 price cut. If they don't, the real prices are half these, so the figures above may be up to 50% too high.
- **DynamoDB provisioned:** 1 read unit and 1 write unit per play per second (about $0.00013 and $0.00065 an hour), at about 70% utilisation. These unit prices are from memory and weren't checked.
- **Valkey (ElastiCache):** one atomic read-and-update script per play. It assumes about 50,000 plays a second per cache.r7g.large node (about $0.14/h), with a replica for each node to take over if it fails.

## Latency is not CPU time

From the client's point of view a play will probably take tens of milliseconds. Almost none of that is CPU time on our instances:

| Step | Rough time | Uses our CPU? |
|---|---|---|
| Client to AWS and back over the internet (plus TLS on a new connection) | 10–50 ms | No, waiting |
| Load balancer | ~1 ms | No |
| DynamoDB read and conditional write | ~5–20 ms | No, waiting |
| Spring, JSON and the database client | ~0.5 ms | Yes |
| Game logic | ~0.001 ms | Yes |

While a request waits, its instance serves other requests, so waiting doesn't need more instances. What it does need is more requests in flight at once. By Little's law, requests in flight = plays a second × time per play. At about 2,600 plays a second per instance and about 20 ms on the server, that's about 52 requests in flight per instance, well within Tomcat's default 200 threads. Thread count would only become the limit with tens of milliseconds on the server at high throughput. Even then the fix is configuration, such as virtual threads (supported by JDK 25 and current Spring Boot), not more instances.

## How sensitive this is

The 500 µs of CPU per play is the guess everything else rests on. Compute a month at much less generous figures (instance counts in brackets; 65% CPU, at least 2 instances):

| CPU per play | 1,000 plays a second | 10,000 | 100,000 | 1,000,000 |
|---|---|---|---|---|
| 0.5 ms (the assumption above) | ~$106 (2) | ~$210 (4) | ~$2,100 (39) | ~$20,000 (385) |
| 2.5 ms (5× worse) | ~$106 (2) | ~$1,100 (20) | ~$10,000 (193) | ~$102,000 (1,924) |
| 10 ms | ~$420 (8) | ~$4,100 (77) | ~$41,000 (770) | ~$407,000 (7,693) |
| 20 ms (the whole round trip spent on CPU, which is unrealistic) | ~$850 (16) | ~$8,100 (154) | ~$81,000 (1,539) | ~$814,000 (15,385) |

Even in the extreme case, compute at 1,000,000 plays a second costs about the same as provisioned DynamoDB (~$810,000) and much less than on-demand (~$3.9 million). The conclusions below hold at every row.

## What the numbers say

1. **The game logic is irrelevant to cost.** It's at most about 0.2% of the CPU per play, and about 0.005% of the tens of milliseconds a player waits. Even making the slowest colour representation 10× faster would barely change the number of instances. This supports keeping colours as names for readability.
2. **Storing the floats costs more than the compute at every rate.** On DynamoDB on-demand it's about 4× the compute at 100 plays a second, and about 190× at 1,000,000. If this ever had to be cheap at scale, that's where to optimise.
3. **The cheap options are less safe with money.** Valkey copies updates to its replica asynchronously, so a failover can lose the last few float updates. That isn't acceptable for real money without a durable ledger behind it. Another option is to send each machine's plays to the one instance that owns it and keep its float in memory, but that also needs a durable log. Both are out of scope for this assignment.

## Not included

- **The load balancer:** about $20 a month at low rates, rising to thousands at 1,000,000 plays a second, where the data it processes dominates.
- **Data transfer to the internet:** about $0.05–0.09 per GB. At 1,000,000 plays a second with 0.5 KB responses, that's about 1.3 PB a month, tens of thousands of dollars.
- **Reserved instances or savings plans**, which could cut compute by about 30–50%.

## Sources

Prices were checked on 2026-10-05:

- [c7g.large price (Holori)](https://calculator.holori.com/aws/ec2/c7g.large/us-east-1)
- [AWS: DynamoDB reduces prices for on-demand throughput](https://aws.amazon.com/about-aws/whats-new/2024/11/amazon-dynamo-db-reduces-prices-on-demand-throughput-global-tables)
- [AWS Database Blog: DynamoDB price reduction](https://aws.amazon.com/blogs/database/new-amazon-dynamodb-lowers-pricing-for-on-demand-throughput-and-global-tables/)
- [DynamoDB on-demand pricing (usage.ai)](https://www.usage.ai/blogs/aws/database-savings-plans/dynamodb/on-demand-pricing/)
- [ElastiCache pricing explained, 2026 (Upstash)](https://upstash.com/blog/aws-elasticache-pricing-explained-2026-full-cost-breakdown)
