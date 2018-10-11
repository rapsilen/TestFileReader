#!/bin/sh

# script to decrypt the gpg files in the given folder
# usage: DecryptionBatch <source> <destinaltion>


AppName="FileDecrypt"
RemoveFile="rm -f "
UnzipFile="tar -zxvf "
ChDir="cd "
Usage="this script is to transfer the hygon kdf/wat/smap files to a destination file\n
a typical source file contains SORT/WAT/SMAP etc\n
the steps to proceed files are:\n 
1)this script will first try decrypt the gpg file and then extract the tar.gz file and then remove the gpg and tar.gz files\n
2)move all the kdf files to the cooresponding folder while a timestamp will be append to the file name\n
  eg. SORT/Lot/abc.xxx.kdf will move to destination/SORT/abc.xxx.kdf.20180101010101, skip the lot dir\n
  and then clean up all the empty kdf lot folder\n
3)move all the dis files to the cooresponding folder while a timestamp will be append to the file name\n
  e.g WAT/Lot/abc.dis will move to destination/WAT/Lot/abc.xxx.dis.20180101010101\n
  and then remove the empty lot folder\n
4)  

  
$0 SourceFile DestinationFile"


Move="mv "
RMDIR="rmdir --ignore-fail-on-non-empty "
REMOVEFile="rm -f "

SORT_PATH="/SORT/"
WAT_PATH="/WAT/"
SMAP_PATH="/SMAP/"
ENG_PATH="/EngSORT"

if [ ! $# -eq 2 ];then
    echo $Usage
    echo "please set the source and destination file"
    exit 1
fi

TIME=$(date "+%Y%m%d%H%M%S")
echo "start task on $TIME"
DATE=`expr substr $TIME 1 8`
AppPath=`pwd $0`

cd $1
if [ $? = 0 ]; then
    SourcePath=`pwd`
    cd $AppPath
    if [ $? = 0 ]; then
        cd $2
        if [ $? = 0 ]; then
            DestPath=`pwd`
        else
            echo "path $2 doesn't exist"
            exit 1
        fi
    fi
else
    echo "path $1 doesn't exist"
    exit 1
fi

echo "full app path is $AppPath"
echo "full source path is $SourcePath"
echo "full destination path is $DestPath"

change_dir_to_sort()
{
    path=$SourcePath$SORT_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo `pwd`
    else
      exit 1
    fi
}

change_dir_to_eng()
{
    path=$SourcePath$ENG_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo `pwd`
    else
      exit 1
    fi
}

change_dir_to_wat()
{
    path='pwd'
    path=$SourcePath$WAT_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo `pwd`
    else
      exit 1
    fi
}
change_dir_to_smap()
{
    path='pwd'
    path=$SourcePath$SMAP_PATH
    $ChDir$path
    if [ $? = 0 ]; then
      echo `pwd`
    else
      exit 1
    fi
}


#Decrypt and extract

decrypt_extract()
{
    if [ $? -ne 0 ]; then
        return 1;
    fi
    #for i in `ls ./ | sed "s:^:$1/:" | grep .gpg`
    for i in `ls ./ | grep .gpg`
    do
        KeepGoing=0
        fullFile=`echo $i | sed 's/.gpg$//'`    # generate fileName without ".gpg"
        fullFile="./$fullFile"
        # delete this file if exist
        if [ -f "$fullFile" ]; then
          $RemoveFile$fullFile
          if [ $? = 0 ]; then
            KeepGoing=1
            echo "successed to remove the existing tar.gz file $fullFile"
          fi
        else
          KeepGoing=1
        fi

        if [ $KeepGoing = 0 ]; then
          continue;
        fi

        KeepGoing=0
        gpg --passphrase QazWsx123 -o $fullFile -d "./$i"    # decryption
        if [ $? = 0 ]; then
          KeepGoing=1
          echo "successed to proceed the gpg file: $i"
          $RemoveFile$i
          if [ $? = 0 ]; then
            echo "successed to remove the gpg file: $i"
          else
            echo "failed to remove this file: $i"
          fi
        else
          echo "failed to proceed the gpg file: $i"
        fi
        echo ""
    done

    for file in `ls ./ | grep .tar.gz`
    do
      zipFile="./$file"
      $UnzipFile$zipFile
      if [ $? = 0 ]; then
        echo "successed to extract this file: $file"
        $RemoveFile$zipFile
        if [ $? = 0 ]; then
          echo "successed to remove the tar.gz file: $file"
        else
          echo "failed to remove this file: $file"
        fi
      else
        echo "failed to execute this command $UnzipFile$zipFile"
        echo "failed to extract this file: $file"
      fi
      echo ""
    done
}


move_sort_kdf()
{
    for file in `ls ./`
    do
      if [ -d $file ]; then
        for kdfFile in `ls ./$file | grep .kdf`
          do
            fullKDF="./$file/$kdfFile"
            # move kdf file to destination
            mvCmd="$Move$fullKDF $DestPath$SORT_PATH$kdfFile.$TIME"
            $mvCmd
            if [ $? = 0 ]; then
              echo "successed to move kdf file $kdfFile to $DestPath$SORT_PATH"
            else
              echo "failed to execute command $mvCmd"
              echo "failed to move $kdfFile to $DestPath$SORT_PATH"
            fi
            echo ""
          done
      fi
    done
}

move_eng_kdf()
{
    for file in `ls ./`
    do
      if [ -d $file ]; then
        for kdfFile in `ls ./$file | grep .kdf`
          do
            fullKDF="./$file/$kdfFile"
            # move kdf file to destination
            mvCmd="$Move$fullKDF $DestPath$ENG_PATH$kdfFile.$TIME"
            $mvCmd
            if [ $? = 0 ]; then
              echo "successed to move kdf file $kdfFile to $DestPath$ENG_PATH"
            else
              echo "failed to execute command $mvCmd"
              echo "failed to move $kdfFile to $DestPath$ENG_PATH"
            fi
            echo ""
          done
      fi
    done
}

move_dis()
{
    for file in `ls ./`
    do
      if [ -d $file ]; then
        for kdfFile in `ls ./$file | grep .dis`
          do
            fullKDF="./$file/$kdfFile"
            # move kdf file to destination
            # check if the lot dir exist
            lotPath="$DestPath$WAT_PATH$file/"
            mkdir -p $lotPath
            if [ $? = 0 ]; then
                mvCmd="$Move$fullKDF $lotPath$kdfFile.$TIME"
                $mvCmd
                if [ $? = 0 ]; then
                  echo "successed to move dis file $kdfFile to $lotPath"
                else
                  echo "failed to execute command $mvCmd"
                  echo "failed to move $kdfFile to $lotPath"
                fi
                echo""
            else
                echo "failed to mkdir for path: $lotPath"
            fi
          done
      fi
    done
}

move_smap()
{
    
    
    for file in ./*
    do
      if [ -d $file ]; then
        removeXls="rm -f ./$file/*xlsx"
        $removeXls
        for kdfFile in $(ls ./$file)
          do
            fullKDF="./$file/$kdfFile"
            #echo $fullKDF
            # move kdf file to destination
            # check if the lot dir exist
            lotPath="$DestPath$SMAP_PATH$file/"
            mkdir -p $lotPath
            if [ $? = 0 ]; then
                mvCmd="$Move$fullKDF $lotPath$kdfFile.$TIME"
                $mvCmd
                if [ $? = 0 ]; then
                  echo "successed to move smap file $kdfFile to $lotPath"
                else
                  echo "failed to execute command $mvCmd"
                  echo "failed to move $kdfFile to $lotPath"
                fi
                echo""
            else
                echo "failed to mkdir for path: $lotPath"
            fi
          done
      fi
    done
    
}

remove_rmap()
{
    for file in `ls ./`
    do
      if [ -d $file ]; then
        for rmapFile in `ls ./$file | grep .rmap`
        do
          # remove all rmaps
          fullFile="./$file/$rmapFile"
          $REMOVEFile$fullFile
          if [ $? = 0 ]; then
            echo "successed to remove rmap file: $rmapFile"
          else
            echo "failed to remove rmap file: $rmapFile"
          fi
          echo ""
        done

      fi
    done
}

clean_up()
{
    for file in `ls ./`
    do
      if [ -d $file ]; then
        count=`ls ./$file |wc -w`
        if [ $count -ne 0 ]; then
          echo "$file size $count"
        else
          fullPath="./$file"
          $RMDIR$fullPath
          echo "rm cmd: $RMDIR$fullPath"
          if [ $? = 0 ]; then
            echo "successed to remove this empty dir: $file"
          else
            echo "failed to execute this cmd: $RMDIR$fullPath"
            echo "failed to remove this empty dir: $file"
          fi
          echo ""
        fi
      fi
    done
}

change_dir_to_sort
decrypt_extract $SORT_PATH
move_sort_kdf
remove_rmap
clean_up

change_dir_to_eng
decrypt_extract $ENG_PATH
move_eng_kdf
remove_rmap
clean_up

change_dir_to_wat
decrypt_extract $WAT_PATH
clean_up
move_dis
clean_up

change_dir_to_smap
decrypt_extract $SMAP_PATH
move_smap
clean_up