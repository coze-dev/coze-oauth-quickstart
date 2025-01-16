#!/bin/bash

# Range over all languages folders
echo "Copying shared files..."
for lang in python go java js; do
    if [ ! -d "$lang" ]; then
        continue # skip if not exist
    fi

    for dir in "$lang"/*; do
        echo "Processing $dir"
        if [ -d "$dir" ]; then
            source_bootstrap_file="$lang/bootstrap.sh"
            if [ -f "$source_bootstrap_file" ]; then
                cp -f $source_bootstrap_file "$dir/bootstrap.sh"
                echo "  Copied $source_bootstrap_file to $dir/bootstrap.sh"
            fi
            for shared_dir in shared/*; do
                shared_dirname=$(basename "$shared_dir")
                cp -rf "$shared_dir" "$dir/"
                echo "  Copied $shared_dir to $dir/$shared_dirname"
            done
        fi
    done
done

echo "Done"
