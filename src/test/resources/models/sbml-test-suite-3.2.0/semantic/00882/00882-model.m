(* 

category:      Test
synopsis:      Linear chain of reactions in one compartment 
using csymbol time within a math expression.
componentTags: Compartment, Species, Reaction, Parameter, CSymbolTime 
testTags:      Amount, ConstantSpecies, BoundaryCondition
testType:      TimeCourse
levels:        2.1, 2.2, 2.3, 2.4, 2.5, 3.1
generatedBy:   Numeric

The model contains one compartment called C.  There are four
species named S1, S2, S3 and S4 and three parameters named k1, k2 and k3.
Species S1 is labeled as constant and a boundaryCondition and therefore 
cannot be changed.  The model contains three reactions defined as:

[{width:30em,margin: 1em auto}|  *Reaction*  |  *Rate*  |
| S1 -> S2 | $k1 * S1 * C * time$  |
| S2 -> S3 | $k2 * S2 * C * time$  |
| S3 -> S4 | $k3 * S3 * C * time$  |] 
where the symbol 'time' denotes the current simulation time.

The initial conditions are as follows:

[{width:30em,margin: 1em auto}|       |*Value*          |*Units*  |
|Initial amount of S1                |$1$  |mole           |
|Initial amount of S2                |$0$              |mole           |
|Initial amount of S3                |$0$              |mole           |
|Initial amount of S4                |$0$              |mole           |
|Value of parameter k1               |$0.4$            |second^-2^     |
|Value of parameter k2               |$0.3$            |second^-2^     |
|Value of parameter k3               |$0.2$              |second^-2^     |
|Volume of compartment C |$1$              |litre          |]

The species values are given as amounts of substance to make it easier to
use the model in a discrete stochastic simulator, but (as per usual SBML
principles) their symbols represent their values in concentration units
where they appear in expressions.

*)

newcase[ "00882" ];

addCompartment[ C, size -> 1 ];
addSpecies[ S1, initialAmount -> 1, constant -> True, boundaryCondition-> True];
addSpecies[ S2, initialAmount -> 0];
addSpecies[ S3, initialAmount -> 0];
addSpecies[ S4, initialAmount -> 0];
addParameter[ k1, value -> 0.4 ];
addParameter[ k2, value -> 0.3 ];
addParameter[ k3, value -> 0.2 ];
addReaction[ S1 -> S2, reversible -> False,
	     kineticLaw -> k1 * S1 * C*\[LeftAngleBracket]time, "time"\[RightAngleBracket] ];
addReaction[ S2 -> S3, reversible -> False,
	     kineticLaw -> k2 * S2 * C*\[LeftAngleBracket]time, "time"\[RightAngleBracket] ];
addReaction[ S3 -> S4, reversible -> False,
	     kineticLaw -> k3 * S3 * C*\[LeftAngleBracket]time, "time"\[RightAngleBracket] ];

makemodel[]