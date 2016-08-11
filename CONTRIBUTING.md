# How to contribute

Third-party patches and inputs are essential for keeping cy3sbml great. 
We want to keep it as easy as possible to contribute changes that
get things working in your environment. There are a few guidelines that we
need contributors to follow so that we can have a chance of keeping on
top of things.

## Getting Started

* Make sure the issue is not already fixed in the development version, i.e.
try to reproduce the issue with the development version
* Make sure you have a [GitHub account](https://github.com/signup/free)
* [Submit the issue on github](https://github.com/matthiaskoenig/cy3sbml/issues), assuming one does not already exist.
  * Clearly describe the issue including steps to reproduce when it is a bug.
  * Make sure you fill in the earliest version that you know has the issue.
* Fork the repository on GitHub

## Making Changes

* Create a topic branch from where you want to base your work.
  * This is usually the develop branch.
  * To quickly create a topic branch based on master; `git checkout -b
    fix/develop/my_contribution master`. Please avoid working directly on the
    `master` or `develop` branch.
* Make commits of logical units.
* Make sure your commit messages are in the proper format.
* Make sure you have added the necessary tests for your changes.
* Run _all_ the tests to assure nothing else was accidentally broken.

## Making Trivial Changes

### Documentation

For changes of a trivial nature to comments and documentation, it is not
always necessary to create a new issue on github. In this case, it is
appropriate to start the first line of a commit with '(doc)'.

## Submitting Changes

* Push your changes to a topic branch in your fork of the repository.
* Submit a pull request to the cy3sbml repository
* The core team looks at Pull Requests on a regular basis and merges them
with develop

# Additional Resources

* [General GitHub documentation](https://help.github.com/)
* [GitHub pull request documentation](https://help.github.com/send-pull-requests/)
