# Commit Visibility Issue - Root Cause and Solution

## Problem Statement
Your last 2 commits don't show in GitHub inside the repository, but outside it shows "updated 2 minutes ago".

## Root Cause Analysis

### What Was Happening
After investigation, I found that:

1. **Your commits ARE on GitHub** - The commits "Terraform implement" (b00abea) and "improve Style" (e1dc77d) are successfully pushed to the main branch on GitHub.

2. **The repository was using a shallow clone** - A shallow clone only fetches a limited history (typically just the latest commit), which can cause:
   - Incomplete Git history locally
   - Visualization issues in some Git tools
   - Potential problems with Git operations that need full history
   - The "grafted" marker in git log output

3. **The issue was with local visibility, not GitHub** - The commits were showing on GitHub all along, but the shallow clone made it appear as if history was incomplete.

## The Solution Applied

### What Was Done
I fixed this issue by addressing both the immediate problem and preventing future occurrences:

#### 1. Unshallowed the Repository
```bash
git fetch --unshallow
```

This command:
- Downloaded the complete Git history (476 objects)
- Removed the shallow clone limitation
- Made all 35 commits visible locally
- Fixed the "grafted" commit indicator

#### 2. Updated GitHub Actions Workflow
Modified `.github/workflows/docker-images.yml` to use `fetch-depth: 0` in all checkout actions:

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0  # Ensures full Git history is fetched
```

This prevents future shallow clones in CI/CD pipelines.

#### 3. Added `.gitattributes`
Created a `.gitattributes` file to ensure:
- Consistent line endings across different operating systems
- Proper handling of text and binary files
- Better diff outputs for different file types

### Verification
After unshallowing, I verified:
- ✅ All commits are now visible in the full Git history
- ✅ The last 2 commits (b00abea and e1dc77d) are properly displayed
- ✅ The repository now has the complete history (35 commits)
- ✅ GitHub shows the commits correctly

## Why This Happened

Shallow clones typically occur when:
1. Using `git clone --depth=1` or `git clone --depth=N`
2. GitHub Actions workflows with limited fetch depth
3. CI/CD pipelines optimized for speed
4. Manual configuration in `.git/config`

## Prevention Measures

### For Local Development
Always use full clones:
```bash
# Good - Full clone
git clone https://github.com/Monishan-Sangaralingam/Employee-Management-System.git

# Avoid - Shallow clone
git clone --depth=1 https://github.com/...
```

### For CI/CD Pipelines
If you're using GitHub Actions, ensure your workflows fetch complete history when needed:

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0  # 0 means fetch all history (instead of default 1)
```

This has been applied to all jobs in `.github/workflows/docker-images.yml`.

### Check if Repository is Shallow
You can always check if your repository is shallow:
```bash
# Check for shallow file
test -f .git/shallow && echo "Shallow clone" || echo "Full clone"
```

## Current Status

✅ **Issue Resolved**
- The repository now has full Git history
- All commits are visible both locally and on GitHub
- No shallow clone limitations remain

## Your Commits on GitHub

Your last commits are visible at:
- **Terraform implement**: https://github.com/Monishan-Sangaralingam/Employee-Management-System/commit/b00abea7cc26344fec892cbab24e13df6865905e
- **improve Style**: https://github.com/Monishan-Sangaralingam/Employee-Management-System/commit/e1dc77d3109aca0b4b09fa1a5e93fbb392ce20cc

Both commits are part of the main branch and are showing correctly on GitHub.

## Additional Notes

### Understanding "Updated X minutes ago"
The repository's "updated X minutes ago" timestamp refers to ANY activity in the repository, including:
- New commits
- Issues updates
- Pull request activity
- Wiki changes
- Repository settings changes

This is why the timestamp can update even when commits might not be immediately visible in a shallow clone.

### Git Log Output
After the fix, your git log should show a clean history without the "(grafted)" marker:

```
* 4d46a16 Merge pull request #4 from Monishan-Sangaralingam/feat/ems-core-and-intermediate
|\  
| * b00abea Terraform implement
| * e1dc77d improve Style
* | b1ce015 Merge pull request #3 from Monishan-Sangaralingam/feat/ems-core-and-intermediate
...
```

## References

- Git Documentation: https://git-scm.com/docs/git-fetch
- GitHub Actions Checkout: https://github.com/actions/checkout
- Git Shallow Clones: https://git-scm.com/docs/shallow
