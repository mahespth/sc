#!/bin/ksh93

################################################################################
#                                                                              #
#  Script :  secure-commands (SC)
#  Args   :  see usage
#  Purpose:  granular SUDO support without running a single sudo command
#                                                                              #
################################################################################

#	
############################################################
# To do:
############################################################
# Test suite with regression test tool to test random flags
# along with valid flags and objects.
#


# Configuration file format (comments allowed)
#	{unix group}	{objectname}
#
# Example configuration
############################################################
# $HOME/.securetruss
# oatusers        /usr/es/sbin/cluster/clstrmgr
#
# sudo -u wasadmin /usr/local/bin/securetruss -l
# oatusers        /usr/es/sbin/cluster/clstrmgr
# oatusers        11337844-/usr/es/sbin/cluster/clstrmgr
#
# sudo -u wasadmin /usr/local/bin/securetruss  -r all -ff -w all -p 11337844
#
#
#
# Example configuration
############################################################
# $HOME/.securevi
############################################################
# oatusers /opt/IBM/WebSphere/WAS85/AppServer/etc/config/xforms/xda/compatibility.xml
# oatusers /opt/IBM/WebSphere/WAS85/AppServer/etc/config/xforms/xda/compt.xmlo
# dev	   /opt/IBM/WebSphere/WAS85/AppServer/etc/config/somefile 
############################################################
#
#
# Output from editing a file
############################################################
#	--- /opt/IBM/WebSphere/WAS85/AppServer/etc/config/xforms/xda/compatibility.xml.securevi 2015-03-20 12:00:00.000000000 +0000
#	+++ /opt/IBM/WebSphere/WAS85/AppServer/etc/config/xforms/xda/compatibility.xml  2014-07-15 12:09:06.000000000 +0000
#	@@ -3,4 +3,3 @@
#	    <versionGroup beginningVersion="6.0.2.0" canonicalName="6.0.2.x"/>
#	    <versionGroup beginningVersion="6.1.0.0" canonicalName="6.1.x"/>
#	  </versionGroups>
#	- # SGM
#	No differences encountered
#	--- /opt/IBM/WebSphere/WAS85/AppServer/etc/config/xforms/xda/compatibility.xml.securevi 2015-03-20 12:02:47.000000000 +0000
#	+++ /opt/IBM/WebSphere/WAS85/AppServer/etc/config/xforms/xda/compatibility.xml  2015-03-20 12:01:24.000000000 +0000
#	@@ -3,3 +3,4 @@
#	    <versionGroup beginningVersion="6.0.2.0" canonicalName="6.0.2.x"/>
#	    <versionGroup beginningVersion="6.1.0.0" canonicalName="6.1.x"/>
#	  </versionGroups>
#	+ # SGM


# Version information from RCS please
############################################################
VERSION='1.45'; VERSION=${VERSION//R*: /}; VERSION=${VERSION//\$/}

# Functions
############################################################

# Root documentation
############################################################
function _documentation_1
{
	cat <<-EOF
	${ProgName} Version: ${VERSION} Installation Documentation
	######################################################################	

	Welcome to the secure-commands, the following guide will assist in the
	configuration and usage of the commands.

	This package extends the usage of SUDO by enabling non system users to configure their own particular access requirements for their own application set. We ensure that security is maintained by keeping a total seperation of duties, logging all object access without the need for overly complex incomprehensible SUDO rules.  We work by adding base SUDO rule access to this command, then making further decisions by parsing the required command and objects that the user seeks to access.  We have simplified the calling of the the package by using symbolic links so that the adminisatraor or user can clearly define shortcuts with their intended objects. This allows for a frictionless experience for developers, user and operators.

	
	# 1. Configuration of the SUDO commands.
	######################################################################	

	The SUDO commands can be added to the system by running the following command as the 'root' user. This will update the SUDO configuration or if appropriate add a new entry in a sudoers include directory should one be specified in the configuration. The SUDO configuration should be accessible at /etc/sudoers.

	# ${DirName}/${ProgName} --sudoconfig

	By default all of the SUDO entries will be added on individual lines, if you have a requirement to keep them all on one line you can export the following environment setting before running the the command above.

	export SUDO_COMPRESSED=true

	To remove the rules from either the configuration file or directory then run the following.

	# ${DirName}/${ProgName} --sudounconfig

	
	# 2. Configuration of sysmbolic links
	############################################################
	
	The package works mainly by determining the name of the application that has been executed and then determining the options that are allowable by that particular application. This allows the maintainance of only one executable, with the configuration data to execute all the other requirements. Having links, symbolic or otherwise allows us to shortcut command line options and create an experience with less friction.  If you plan to use alternative link definitions as described below then there is not a need to configure the symbolic links if you have no plan to use them in this way.

	To configurat the Links to the target.

	# ${DirName}/${ProgName} --configure

	To un-configure.

	# ${DirName}/${ProgName} --unconfigure


	Alternative syntax is available for linking the files to allow simplified
	calling of options. The form is;

		user.command  -> target
		or
		command.user  -> target

	You can utilise these links as per the example blow;
	ln -s ${DirName}/${ProgName} $HOME/bin/wasadmin.ops
	ln -s ${DirName}/${ProgName} $HOME/bin/wasadmin.ls
	ln -s ${DirName}/${ProgName} $HOME/bin/wasadmin.vi

	ln -s ${DirName}/${ProgName} /usr/local/bin/bin/wasadmin.ops
	ln -s ${DirName}/${ProgName} /usr/local/bin/wasadmin.ls
	ln -s ${DirName}/${ProgName} /usr/local/bin/wasadmin.vi
	
	And execution of he the desired commands can be called using the short form as an aternative to calling the fully qualified path.
	wasadmin.ls -lR /opt/IBM/WebSphere/
	iibadmin.ops mqsistopbroker EB1.BROKER

	Alternative commands would be.
	sudo -u wasadmin ${DirName}/securevi -lR /opt/IBM/WebSphere
	sudo -u iibadmin ${DirName}/secureops mqsistoprboker EB1.BROKER


	Copy and Move
	Copy and move configurations allow wildcards, however they must not resolve to more than two unique filenames. The configuration must specify the source and the target following the normal rules for configuraiton.

	EOF
}

function _saveERC
{
	[[ -n ${DEBUG} ]] && set -x

	typeset -i ERC=$1
	typeset    ercFile=${TMPDIR}/${ProgName}.${myPID}

	if [[ -e ${ercFile} && -w ${ercFile} ]]
	then
		if ! rm -f ${ercFile}
		then
			_errMsg "File ${ercFile} sould not exist, stopping."
			exit 1
		fi
	fi

	# Create the stub
	############################################################
	>${ercFile}

	# If we cant chmod the file then we dont own it and its likely we have
	# been raced to it !
	############################################################
	if ! chmod 700 ${ercFile} 
	then
		_errMsg "File ${ercFile} has been modified by someone else, stopping."
		exit 1
	fi

	echo "${ERC}" >${ercFile}

	return 0
}

function _restoreERC
{
	typeset -i ERC=
	typeset    ercFile=${TMPDIR}/${ProgName}.${myPID}

	if [[ ! -e ${ercFile} && -w ${ercFile} ]]
	then
		_errMsg "File ${ercFile} has been modified by someone else, stopping."
		exit 1
	fi

	# so if its blank there was a problem in the code and we dont want
	# to let this go so lets mark it so !
	############################################################
	if [[ ! -s ${ercFile} ]]
	then
		ERC=128
	else
		# Timeout to make sure there is nothing odd going on.
		############################################################
		read -t 30 ERC <${ercFile}
	fi

	if ! rm -f ${ercFile}
	then
		_errMsg "File ${ercFile} has been modified by someone else, stopping."
		exit 1
	fi

	return ${ERC}
}



# We depend on knowing what links to what, so a dedicated
# function to resolve the links is nice and clean
############################################################
function _resolve_link
{
	[[ -n ${DEBUG} ]] && set -x

	typeset   linkName=$1
	typeset   finalDestination=$1
	typeset	  lastKnownDirectory

	while find ${linkName} -type l -print 2>/dev/null | read linkName
	do
		[[ $linkName == @(/*) ]] && lastKnownDirectory=$( dirname $linkName )

		# This will fail if we cannot "stat" the directory, ie if called
		# via sudo and in the users home directory and its 750 or 700
		############################################################
		[[ -z ${lastKnowDirectory} ]] && lastKnowDirectory=$( pwd -P 2>/dev/null )

		# Last ditch, hopefully this works..
		############################################################
		[[ -z ${lastKnowDirectory} ]] && lastKnowDirectory=$PWD

		ls -l ${linkName} \
		| awk '{ print $NF }' \
		| read linkName

		finalDestination=${linkName}
	done
	
	if [[ ${finalDestination} != @(/*) ]]
	then
		print "${lastKnownDirectory}/${finalDestination}"
	else
		print "${finalDestination}"
	fi

	if [[ ! -e ${finalDestination} ]]
	then
		return 1
	fi
		
	return 0
}

# Admin account holder documentation
############################################################
function _documentation_2
{
	:
}

# User documentation
############################################################
function _documentation_3
{
	[[ -n ${DEBUG} ]] && set -x
	
	cat <<-EOF
	${ProgName} Version: ${VERSION} Builtin Commands
	######################################################################	
	EOF

	awk '/# config-commands start/,/# config-commands end/ {
		if ($2 ~ /secureCommand/ )
		{
			gsub("secureCommand=",""); 
			if ($2 ~ /\//) 
			{
				printf("     %-40s\n",$2);
			} else {
				printf("     %-40s (internal)\n",$2);

			}
		}
	} ' ${DirName}/${ProgName} \
	| tr -d "\"'"

}
	
function _errMsg
{
	printf "[E] %s\n" "$@" >&2
}

function _infoMsg
{
	printf "[E] %s\n" "$@" >&2
}

function _opsConfigProfile
{
	[[ -n ${DEBUG} ]] && set -x

	typeset   opsBin=${opsBin:-"/usr/local/opsbin"}
	typeset executionShell=$( lsuser -a shell $USER 2>/dev/null | cut -f2 -d"=" )
	typeset _HOME
	

	if [[ -n $2 && ${USER} == "root" ]]
	then
		_HOME=$( lsuser -a home $2 | cut -f2 -d"=" )
		executionShell=$( lsuser -a shell $2 2>/dev/null | cut -f2 -d"=" )
	else
		_HOME=${HOME}
	fi
		
	case ${executionShell} in
	 */csh)	
		shellInit="${_HOME}/.cshrc"	

		if [[ -s ${shellInit} ]]
		then
			cat <<-EOF >${shellInit}.new
			$( cat ${shellInit} )
		
			# Added by secure Commands $( date ) called by ${REALUSER:-$USER}
			############################################################
			set path = (${path} ${opsBin})
			EOF 

			if [[ $? != 0 ]]
			then
				_errMsg "Problem creating ${shellInit}.new"
			else
	
				cp ${shellInit} ${shellInit}.orig
				cp ${shellInit}.new ${shellInit}

				rm -f ${shellInit}.new

				_infoMsg "${shellInit} updated."
			fi
		else
			_errMsg "${shellInit} is missing, please manually create your required rc file."
			exit 1
		fi
		;;
	 */bash)
		shellInit="${_HOME}/.bash_profile"

		if [[ -s ${shellInit} ]]
		then
			cat <<-EOF >${shellInit}.new
			$( cat ${shellInit} )
		
			# Added by secure Commands $( date ) called by ${REALUSER:-$USER}
			############################################################
			PATH=\$PATH:/usr/local/opsbin
			EOF

			if [[ $? != 0 ]]
			then
				_errMsg "Problem creating ${shellInit}.new"
			else
	
				cp ${shellInit} ${shellInit}.orig
				cp ${shellInit}.new ${shellInit}

				rm -f ${shellInit}.new

				_infoMsg "${shellInit} updated."
			fi
		else
			_errMsg "${shellInit} is missing, please manually create your required rc file."
			exit 1
		fi
		;;

	 */ksh*)
		shellInit="${HOME}/.profile"

		if [[ -s ${shellInit} ]]
		then
			cat <<-EOF >${shellInit}.new
			$( cat ${shellInit} )
		
			# Added by secure Commands $( date ) called by ${REALUSER:-$USER}
			############################################################
			PATH=\$PATH:/usr/local/opsbin
			EOF

			if [[ $? != 0 ]]
			then
				_errMsg "Problem creating ${shellInit}.new"
			else
	
				cp ${shellInit} ${shellInit}.orig
				cp ${shellInit}.new ${shellInit}

				rm -f ${shellInit}.new

				_infoMsg "${shellInit} updated."
			fi
		else
			_errMsg "${shellInit} is missing, please manually create your required rc file."
			exit 1
		fi
		;;
	 '')
		_errMsg "We do not recognise the shell or could not extract your user settings, cannot update your path."
		exit 1
		;;
	 *)
		_errMsg "We do not recognise the shell $executionShell, so cannot update your path."
		exit 1
		;;
	esac

	_infoMsg "Remember to log out and back in again to enable new profile settings."
}

function _opsConfig
{
	[[ -n ${DEBUG} ]] && set -x

	typeset   opsBin=${opsBin:-"/usr/local/opsbin"}
	typeset   secureCommands
	typeset   commands
	typeset   targetAccount
	typeset   targetHome
	typeset   targetFile
	typeset   OK

	if [[ ! -d ${opsBin} ]]
	then
		_infoMsg "Creating ${opsBin}"

		if ! mkdir ${opsBin}
		then
			_errMsg "Sorry could not create ${opsBin}."
			exit 1
		fi

		if ! chown root:system ${opsBin}
		then
			_errMsg "Error changing ${opsBin} owner to root.system"
			exit 1
		fi

		if ! chmod 755 ${opsBin}
		then
			_errMsg "Error changing permisions of ${opsBin}"
			exit 1
		fi
	fi

	# -> also create the help link...

	# Get all users that are not users :)
	############################################################
	lsuser -ca groups gecos home ALL \
	| awk -F":" '!/^#/ && !/:[Uu][Ss][Rr]/ && $2 !~ /staff/ { print $1,$NF }' \
	| while read targetAccount targetHome
	do
		set +o noglob
		 secureCommands=$( ls -1 $targetHome/etc/secure*.cfg $targetHome/.secure* 2>/dev/null | awk -F"/" '{ sub(/(.cfg$|.log$)/,""); sub(/\./,""); if (! seen[$NF]++) {printf "%s ",$NF } }' )
		set -o noglob

		if [[ ! -z ${secureCommands} ]]
		then
			secureCommands="${secureCommands} securehelp"
		fi
	
		for command in ${secureCommands}
		do
			targetFile="${opsBin}/${targetAccount}.${command/secure/}"

			if [[ -e ${SecureDirName}/${command} ]]
			then
				if [[ ! ${SecureDirName}/${command} -ef ${targetFile} ]]
				then
					rm -f ${targetFile}
				fi
					
				if [[ -e ${targetFile} ]]
				then
					if ! find ${targetFile} -user root -perm 200 | read OK
					then
						rm -f ${targetFile}
					fi
				fi
			fi

			ln -fs ${SecureDirName}/${command} ${targetFile}

			_infoMsg "Created: ${targetFile}"
		done
	done
		
	############################################################

	# @@SGM go though users that need a profile update?
}

function _sudoConfig
{
	[[ -n ${DEBUG} ]] && set -x
	
	typeset    sudoRules			# SUDO rules to add 
	typeset -i loop		
	typeset    SUDOERS="/etc/sudoers"
	typeset    HAS_INCLUDE

	HAS_INCLUDE=$( awk '/#includedir/ { print $2 }' ${SUDOERS} )

	_infoMsg "Configuring SUDO rules"

	if ! ${_SUDO} -v
	then
		_errMsg "SUDO rules are already broken !! I refuse to make it worse."
		exit 1
	fi

	# Does secure VI already exist in SUDO
	############################################################
	if grep -q "# -- ${PackageName} start --" ${SUDOERS}
	then
		if ! cp -p ${SUDOERS} ${SUDOERS}.save
		then
			_errMsg "Failed to save ${SUDOERS} to ${SUDOERS}.save, stopping."
			exit 1
		fi

		if ! awk '/# -- '${PackageName}' start/,/# -- '${PackageName}' end/ { next; } { print }' ${SUDOERS}.save >${SUDOERS}
		then
			_errMsg "Failed to remove existing SUDO rule, stopping."
			exit 1
		fi

		_infoMsg "Removed existing SUDO rule"
	fi

	/usr/bin/egrep '[[:space:]][^\$]secureCommand=' ${DirName}/${ProgName}  \
	| cut -f1 -d")" \
	| while read
	do
		typeset linkName="${DirName}/secure${REPLY}"

		sudoRules[${#sudoRules[@]}]="${DirName}/secure${REPLY} *"
		# sudoRules[${#sudoRules[@]}]="${DirName}/secure${REPLY} [! ]*"
	done

	# Add the SUDO data
	############################################################
	(
		print "# -- ${PackageName} start --"

		if [[ $SUDO_COMPRESSED != "true" ]]
		then
			printf "ALL	ALL=(ALL) NOPASSWD: "

			for (( loop=0; loop<${#sudoRules[@]}; loop++ ))
			do
				printf " %s," "${sudoRules[${loop}]}"
			done \
			| sed 's|,$||'

			printf "\n"
		else
			for (( loop=0; loop<${#sudoRules[@]}; loop++ ))
			do
				printf "ALL	ALL=(ALL) NOPASSWD: %s\n" "${sudoRules[${loop}]}"
			done 
		fi

		print "# -- ${PackageName} end --"

	) >>${SUDOERS}

	if [[ $? != 0 ]]
	then
		_errMsg "Failed to update $SUDOERS correctly, please check."
		exit 1
	fi

	_infoMsg "Updated ${SUDOERS} rules"

	if ! ${_SUDO} -v
	then
		_errMsg "SUDO Rules not working - restoring to previous version"
		
		if ! cp -p ${SUDOERS}.save ${SUDOERS}
		then
			_errMsg "Problem restoring SUDO rules, your screwed, hope you got a backup."
			exit 1
		else
			if ${_SUDO} -v
			then
				_errMsg "SUDO still broken - is someone else doing something?"
				exit 1
			else
				_infoMsg "SUDO rules restored OK"
			fi
		fi
	fi
	
	return $?
}

function _sudoUnconfig
{
	[[ -n ${DEBUG} ]] && set -x
	
	typeset    sudoRules			# SUDO rules to add 
	typeset -i loop		
	typeset    SUDOERS="/etc/sudoers"
	typeset    HAS_INCLUDE

	HAS_INCLUDE=$( awk '/#includedir/ { print $2 }' ${SUDOERS} )

	_infoMsg "Unconfiguring SUDO rules"

	if ! ${_SUDO} -v
	then
		_errMsg "SUDO rules are already broken !! I refuse to make it worse."
		return 1
	fi

	# Does secure VI already exist in SUDO
	############################################################
	if grep -q "# -- ${PackageName} start --" ${SUDOERS}
	then
		if ! cp -p ${SUDOERS} ${SUDOERS}.save
		then
			_errMsg "Failed to save ${SUDOERS} to ${SUDOERS}.save, stopping."
			return 1
		fi

		if ! awk '/# -- '${PackageName}' start/,/# -- '${PackageName}' end/ { next; } { print }' ${SUDOERS}.save >${SUDOERS}
		then
			_errMsg "Failed to remove existing SUDO rule, stopping."
			return 1
		fi

		_infoMsg "Removed existing SUDO rule."

		if ! ${_SUDO} -v
		then
			if ! cp -p ${SUDOERS}.save ${SUDOERS}
			then
				_errMsg "Problem restoring SUDO rules, your screwed, hope you got a backup."
				exit 1
			else
				if ${_SUDO} -v
				then
					_errMsg "SUDO still broken - is someone else doing something?"
					return 1
				else
					_infoMsg "SUDO rules restored OK"
				fi
			fi
		fi

	elif [[ -s ${HAS_INCLUDE}/${PackageName} ]]
	then
		if /bin/rm -f ${HAS_INCLUDE}/${PackageName}
		then
			_infoMsg "SUDO Rules removed"
		else
			_errMsg "Problem removing SUDO rules, stopping".
			return 1
		fi
	else
		_infoMsg "SUDO Rules not found."
		return 1
	fi

	return
}

# Remove application Links
###########################################################
function _unconfigureLinks
{
	[[ -n ${DEBUG} ]] && set -x
	
	awk '/# config-commands start/,/# config-commands end/' ${DirName}/${ProgName} \
	| /usr/bin/egrep '[[:space:]][^\$]secureCommand=' \
	| grep -v "[\s]*_" \
	| cut -f1 -d")" \
	| while read
	do
		typeset linkName="${DirName}/secure${REPLY}"

		if [[ -h ${linkName} ]]
		then
			if [[ ${linkName} -ef ${DirName}/${ProgName} ]]
			then
				_infoMsg "${linkName} exists removing"

				if ! rm ${linkName}
				then
					_errMsg "errors trying to remove ${linkName}"
				fi
			fi
		fi
	done

	return 
}

# Configure application Links
############################################################
function _configureLinks
{
	[[ -n ${DEBUG} ]] && set -x
	
	(
		[[  $EXTENDED_CONFIG == true ]] && \
			awk '/# config-commands start/,/# config-commands end/' ${DirName}/${ProgName} \
			| /usr/bin/egrep '[[:space:]][^\$]: secureCommand='

		awk '/# config-commands start/,/# config-commands end/' ${DirName}/${ProgName} \
		| /usr/bin/egrep '[[:space:]][^\$]secureCommand='
	) \
	| grep -v "[\s]*_" \
	| cut -f1 -d")" \
	| while read
	do
		typeset linkName="${DirName}/secure${REPLY}"

		if [[ -h ${linkName} ]]
		then
			if [[ ${linkName} -ef ${DirName}/${ProgName} ]]
			then
				_infoMsg "${linkName} exists already, skipping"
				continue
			else
				_errMsg "[E] ${linkName} is not correct, removing."

				if ! rm ${linkName}
				then
					_errMsg "errors trying to remove ${linkName}, stopping."
					return 1
				fi
			fi

		elif [[ -f ${linkName} ]]
		then
			_infoMsg "${linkName} exists as a file, skipping"
			continue
		fi

		printf "[A] Configuring: %s\n" $linkName

		if ! /usr/bin/ln -s ${DirName}/${ProgName} ${linkName}
		then
			_errMsg "Failed to create ${linkName}, stopping."
			return 1
		fi
		
	done

	return
}

# Get a long getops commands - not yet implemented
############################################################
function _getopts_long
{
	if [[ ! -x $PERL ]] || $PERL -MGetopt::Long -e 1 2>/dev/null
	then
		_errMsg "long flags are not supported on this platform. Please contact BAU if required."
		exit 1
	fi

	typeset requiredOpts="$1"
	typeset suppliedOpts="$2"

	$PERL -MGetopt::Long '
		Getopt::Long::Configure qw(gnu_compat,bundling);

		$gl = new Getopt::Long; if (! $gl->getoptions ) { 1; }

		0;
	'

	return $?
}

# Not implemented
# Usage statement of the command we are trying to run?
############################################################
#function _usage_cmd
#{
#	$secureCmd -?
#}

function _usage
{
	print "VE ${PackageName} ${ProgNameShort} ${VERSION}"
	#print "Please note all access is monitored and reported centrally."
	print ""

	if [[ -z ${SUDO_COMMAND} && ${ProgName} != @(*\.*) ]]
	then
		print "usage: $ProgName: -e"
		print "	-e	edit object configuration file for this user ($CONFIG)."
		print "	-h	full help"
		
		# @@SGM TO IMPLEMENT
		#print " -c	check configuration files for known errors"
		#print " -l	show configuration"
	else
		# This is the default user output
		############################################################

		print "usage: $ProgName: [[ -l ]| [ object ] [ object ] [ ... ] ]"
		print "	-l	list objects (files or processes) that I can work with."
		print ""
		print "	Where the object is a filepath, process or other."
		print "	for help from the target command try again with '-h' or '-?'."

	fi

	if [[ ${OLD_PATH} != @(*opsbin*) ]]
	then
		print ""

		if [[ $USER == "root" ]]
		then
			print "	--update-profile [user]	ensure profile contains /usr/local/opsbin"
		else
			print "	--update-profile	ensure profile contains /usr/local/opsbin"
		fi

		print ""
	fi

	print ""
	print ""

	if [[ $USER == "root" && -z ${SUDO_COMMAND} ]]
	then
		print "Configuration options for sys admin"
		print ""
		print "	Configure SUDO."
		print "		--sudoconfig"
		print ""
		print "	Unconfigure SUDO."
		print "		--sudounconfig"
		print ""
		print "	Configure application links"
		print "		--configure"
		print ""
		print "	Unconfigure application links"
		print "		--unconfigure"
		print ""
		print " Configure /usr/local/opsbin for all current configuration"
		print "		--opsbinconfig"
		print ""
	fi
}

function _logger
{
	[[ -n ${DEBUG} ]] && set -x

	/usr/bin/logger -t "${ProgName}" -i -p user.notice "$@"
}

function _logger_err
{
	[[ -n ${DEBUG} ]] && set -x

	/usr/bin/logger -t "${ProgName}" -i -p auth.notice "$@"
}

#
# Ensure we have control from user interuptions to commands that we execute
############################################################
function _trapcntrlc
{
	[[ -n ${DEBUG} ]] && set -x
	
	# Restore behaviours
	############################################################
	#trap '' 1 2 3 11 15 EXIT
	trap '' 1 2 11 15 EXIT

	return $1
}


# Debug output of KSH* variables and their configuration
############################################################
function _debugVars
{
	[[ -z $DEBUG ]] && return

	typeset _debugVar _debugVarIndex _debugVarType
	
	for _debugVar in $*
	do
		typeset \
		| awk -v V="${_debugVar}" '{
			if (NF == 2 && $2 == V) { print; exit; }
			if (NF == 1 && $1 == V) { print "env "$1; exit; }
			if ($1 == "associative" && $3 == V ) { print "associative "$3; exit; }
			if ($1 == "indexed" && $3 == V ) { print "indexed "$3; exit; }
		}' \
	   	| read _debugVarType junk
	
		[[ -z ${_debugVarType} ]] && _debugVarType="unset"
		
		case ${_debugVarType} in
		 associative)
			for _debugVarIndex in $( eval echo \$\{!${_debugVar}\[\@\]\} )
			do
				 eval echo "[D] ${_debugVar}[\\\"$_debugVarIndex\\\"]=\\\"$( echo $\{${_debugVar}[\"${_debugVarIndex}\"\]\} )\\\""
			done
			;;
		 indexed)
			for _debugVarIndex in $( eval echo \$\{!${_debugVar}\[\@\]\} )
			do
				eval echo "[D] ${_debugVar}[$_debugVarIndex]=\\\"$( echo $\{${_debugVar}[\"${_debugVarIndex}\"\]\} )\\\""
			done
			;;
		 unset)
			echo "[D] ${_debugVarType} ${_debugVar}"
	        	;;
		 *)		
			eval echo "[D]  ${_debugVarType} \${_debugVar}=\\\"\$${_debugVar}\\\""
			;;
		esac
	done
}

# Main
############################################################
# Disable globbing, we dont want any kind of playing about!
############################################################
OLD_PATH=$PATH
PATH=/usr/bin:/usr/sbin:/usr/ucb
export PATH

LANG=C
export LANG

TMPDIR="/tmp"
export TMPDIR

# Paranoid? moi?
############################################################
unset CDPATH
unset LIBPATH
unset MANPATH
unset LD_PRELOAD

# IFS !?

set -o noglob

typeset   ProgName=$( basename $0 ); 
typeset   ProgNameShort=${ProgName/secure/}
typeset   DirName=$( dirname $0 ); [[ $DirName == "." ]] && DirName=$PWD
typeset   SecureDirName='/usr/local/bin'

# Debugging? only as root
############################################################
if [[ $- == @(*x*) || ! -z $DEBUG ]]
then
	if find /tmp/${ProgName}.debug -user root 2>/dev/null | read OK
	then
		if [[ $DEBUG == "extended" ]]
		then
			# This is intentionall split between two lines..
			PS4='
| $LINENO | ${_CODE[$LINENO]} 
| $LINENO | 	[ $0 ] '; 

			typeset _LINE_NO _LINE
			typeset _CODE 

			/usr/bin/cat -n ${DirName}/${ProgName} \
			| while read _LINE_NO _LINE
			do	
				_CODE[${_LINE_NO}]="${_LINE}"
			done

			set -x 
		else
			PS4='$LINENO | $0 ] '; set -x 
			DEBUG=${DEBUG:-"on"}

		fi
	else
		set +x
	fi
fi

# Make sure no one has tampered with my permissions
# again we dont give out to much information!
############################################################

if ! /usr/bin/find $( _resolve_link ${DirName}/${ProgName} ) -user root ! -perm -022 | read OK
then
	print -u2 "FATAL ERROR - APPLICATION PERMISSIONS"; exit 1
fi

if [[ -u $( _resolve_link ${DirName}/${ProgName} ) ]]
then
		print -u2 "FATAL ERROR - APPLICATION PERMISSIONS"; exit 1
fi

if ! find /etc/environment -user root -group system -perm 664 | read OK
then
	print -u2 "FATAL ERROR - ENVIRONMENT PERMISSIONS"; exit 1
fi

# Start of our declarations
############################################################

typeset    PackageName="securecommands"
typeset    TTYNUM TTYID			# Who we are (users real id)
typeset -i ERC=0			# error tracking
typeset    objectName			# the name of the file or object we are going to manage
typeset    objectFilesystem		# Location of the object on the disk, required for encryption 
typeset    objecFilesystemEncrypted 	# This filesystem is under Vormetric guarded encyption
typeset -i objectMaxFiles=1		# the max number of objects allowed for the command
typeset    objectType=read		# type of action, read or write
typeset    objectFlag			# if a flag contains the object then this is the flag id
typeset	   saveArgs=$*			# save the current arguments before any processing
typeset -A args=( $* )			# Array of the arguments for indexed lookup
typeset    argss="$*"			# Strings of the arguments 
typeset    argsParsed			# Processed arguments
typeset -i argIndex=${#args[*]}-1	# number of objects we have indexed
typeset -i _listOnly= 			# are we just listing our capabilities / objects we can use
typeset -i _editOnly= 			# editing of the control files that we own
typeset    _fromUser= 			# user that we are doing to list the objects for
typeset    secureCommand		# the command that we will execute 
typeset    executeCommand		# Command we will execute from an Alias
typeset -A _sourceGroups		# The groups of the user that we will use to check their authorisation
typeset    _longUserName    		# Gecos of the calling ID, required during logging of violations.
typeset    targetUser			# User we plan on actioning the object as
typeset    header="# Group         File/object"
typeset -A configData			# Load the config file in memory.
typeset -A configDataTarget			# Load the config file in memory.
typeset -A shellCmdData			# Load the config command data
typeset -A shellCmdHelp			# Load the config command help data
typeset -A shellCmdAllow		# Load the config for allowed Chars
typeset -A shellCmdLogs			# Load the config logging location
typeset    shellCmdLogsPerms=600	# Permissions for the log files
typeset -A procsData			# Discovered process mapping
typeset -i cmdAlias=0			# Command is an alias if true, so needs eval
typeset -i numberOfFields=0		# Number of fields in a alias string
typeset -i epoc				# The epoc..
typeset    logOutput			# Name of final log file if required
typeset    groupName fileSpec	fileSpecParam	# pointers for config data.
typeset    pid command arguments	# /usr/bin/ps information
typeset    CONFIG
typeset    PERL="/usr/bin/perl"		# Location of perl for getopts_long
typeset    _secureVI_log		# Location of securevi.log 
typeset    _secureLocalConfig		# Local configuraiton location
typeset    PIECE						# $PIECE()
typeset    sourceTarget=false		# Is this a "source" "target" pairing.
typeset -i myPID=$$			# My PID
typeset -i pointer			# Misc re-use pointer

typeset    var							# used in while read
typeset    value						# used in while read

typeset    _SUDO="/bin/sudo"

# We reset these as SUDO can change them.
# we need to eval the "~" as this can fail in install
# or background attempts at running the commands
############################################################
export USER=$( /usr/bin/whoami )
export HOME=$( eval echo ~$USER 2>/dev/null )

if [[ -z ${HOME} ]]
then
	HOME=$( /usr/sbin/lsuser -a home ${USER} | cut -f2 -d= )
	export HOME
fi

if [[ -z ${HOME} ]]
then
	_errMsg "Critical error, stopping."
	exit 1
fi

# Find the configuration data! If it already existing in
# $HOME then use that otherwise check to see if there is
# a users etc directory we can use and we can change the
# name so its not then a hidden file. If no etc directoryu
# exists then revert to the original plan of in $HOME.
############################################################
if [[ -e /usr/local/etc/${ProgName}.cfg && -r /usr/local/etc/${ProgName}.cfg ]]
then
	# We have found a global version of the configuration file.
	# we could set this file and leave nothing in it to disable
	# access to a given tool ! Or we could give a global access
	#
	# The same again below, but with a ${USER}. prefix
	#
	############################################################
	CONFIG=/usr/local/etc/${ProgName}.cfg

elif [[ -e /usr/local/etc/${USER}.${ProgName}.cfg && -r /usr/local/etc/${USER}.${ProgName}.cfg ]]
then
	# A more granular access based on the user name, this would make configuration
	# of common accounts on all boxes much easier to implement.
	############################################################
	CONFIG=/usr/local/etc/${USER}.${ProgName}.cfg
	
elif [[ -e $HOME/.${ProgName} ]]
then
	CONFIG=$HOME/.${ProgName}
elif [[ -e $HOME/etc ]]
then
	CONFIG=$HOME/etc/${ProgName}.cfg
else
	CONFIG=$HOME/.${ProgName}
fi

# Options for VI editing - make sure we cannot shell exec or
# use local init options
############################################################
EXINIT=":set noexrc shell="
export EXINIT


# Ensure we have options (if we are using the user.cmd syntax
# then we need to allow this to pass through)
############################################################
if [[ -z $1 ]]
then
	if [[ ${ProgName} != @(*\.*) ]]
	then
		:
		_usage; exit 0;
	elif [[ ${DirName} == @(*secure*) ]]
	then
		:
		_usage; exit 0;
	else
		_usage; exit 0;
	fi
fi

# We depend on syslog to be available to get security messages delivered
# if there's something missing then we wont run as we are unable to
# log any actions or exceptions
############################################################
if [[ ! -e /dev/log ]]
then
	_errMsg "System security logging is broken please contact BAU support asking them to investigate SYSLOG issues."
	exit 1
fi


# If the filename is in the format of "." and its a LINK then we
# using this form to indicate the user and the command to be run
# by the sudo operation. This could save massive amounts of time
# writing sudo lines ex:
#
# exmample:
# 	was:	sudo -u wasadmin /usr/local/bin/securevi /tmp/bob
#	now:	wasadmin.vi /tmp/bob
#	now:	mqm.ops /var/mqm/MQScripts/stop_mq.sh SP.EB01.MANAGER 10
#	now:	was.ops /var/was85/scripts/bin/enableMMR.sh showonly
############################################################
if [[ $ProgName == @(*\.*) && -L $0 ]]
then
	PIECE=( ${ProgName//\./ } )			# Split the fields
	
	if [[ ! -z "$( id -u ${PIECE[0]} 2>/dev/null )" && ! -z "$( id -u ${PIECE[1]} 2>/dev/null )"  ]]
	then
		_errMsg "Ambiguous filename, both are valid user accounts."
	fi

	if [[ ! -z "$( id -u ${PIECE[0]} 2>/dev/null )" ]]
	then
		execCommand="${SecureDirName}/secure${PIECE[1]/secure/}"
		targetUser=${PIECE[0]}

		
	elif [[ ! -z "$( id -u ${PIECE[1]} 2>/dev/null )" ]]
	then

		execCommand="${SecureDirName}/secure${PIECE[0]/secure/}"
		targetUser=${PIECE[1]}
	fi

	if [[ ! -z ${targetUser} ]]
	then
		# Re-execute this command if we have access to run it
		# as the desired user and secure command.
		############################################################
		if ${_SUDO} -n -ll 2>/dev/null | grep -qw "${execCommand}"
		then
			# Change to a directory we know we wont have issues with "stat" for 750 permissions
			cd /tmp; exec ${_SUDO} -u ${targetUser} ${execCommand} $*
		else
			_errMsg "We do not believe you have access to execute ${execCommand}."
			exit 1
		fi
	fi

	_errMsg "Failed to identify target user or targer user not local to this system."
	
	exit 1

# Use the directory name as the shortcut?
############################################################
elif [[ ${DirName##*/} == @(*secure*) ]]
then

	DirName=${DirName##*/}					# Its safe to replace this as we are not reusing
	PIECE=( ${DirName//\./ } )			# Split the fields
	
	if [[ ! -z "$( id -u ${PIECE[0]} 2>/dev/null )" && ! -z "$( id -u ${PIECE[1]} 2>/dev/null )"  ]]
	then
		_errMsg "Ambiguous filename, both are valid user accounts."
	fi

	# If the format is user.command, otherwise its command.user, or broke
	############################################################
	if [[ ! -z "$( id -u ${PIECE[0]} 2>/dev/null )" ]]
	then
		execCommand="${SecureDirName}/secure${PIECE[1]/secure/}${ProgName}"
		targetUser=${PIECE[0]}

		
	elif [[ ! -z "$( id -u ${PIECE[1]} 2>/dev/null )" ]]
	then

		execCommand="${SecureDirName}/secure${PIECE[0]/secure/}${ProgName}"
		targetUser=${PIECE[1]}
	fi
	
	# Re-execute this command if we have access to run it
	# as the desired user and secure command.
	############################################################
	if [[ ! -z ${targetUser} ]]
	then
		if ${_SUDO} -n -ll 2>/dev/null | grep -qw "${execCommand}"
		then
			exec ${_SUDO} -u ${targetUser} ${execCommand} $*
		else
			_errMsg "We do not beleive you have access to execute ${execCommand}."
			exit 1
		fi
	fi

	_errMsg "Failed to identify target user or targer user not local to this system."
	
	exit 1
		
elif [[ $ProgName == @(secure*) ]]
then

	# List of supported Commands and their options:
	#
	# @@@@ NOTE @@@@
	#
	# Please keep the format below of
	#	name) secureCommand='';
	# as this is used to configure the app.
	#
	# If allowedOpts='' then no falgs can be used
	#    allowedOpts must be a valid getopts (not getopt) string
	#                long getops must be processed by a perl module.
	# 
	# If objectType == readonly, then we execute the code as we know
	#		nothing is going to change as a reuslt of the command
	#		so you need to make sure you calsify these commands 
	#		carefully and ensure that any flags that could create
	#		a change are removed.
	#
	# If objectType == write, this is more for editing commands such as ed
	#		or vi, something that we want to take a copy of the file
	#		and check for changes and store away the difference
	#
	# If objectType == process, this is for matching object names to a process
	#		id of a running process, and if its not running then 
	#	 	non are listed in the "-l" list. And you can do jack.
	#		we use this so user can run commands such as the proc* 
	#		utilities or truss or kill etc. 
	#
	# What is : before secureCommand?? The configuration option
	# will not link any of these commands automatically when its run
	# you will have to manually link them (as we're not sure if they
	# have a real demand) or run the --config with the EXTENDED_CONFIG 
	# variable set to true.
	#
	############################################################.

	# 
	# Alternative configuration data!?
	# We can create a root owned stanza file with only root
	# update ability and use this as a valid configuration seed.
	# But note... you wont get a link created by me and you also
	# will not get a sudo rule created.
	############################################################
	
	typeset -l var								# force lowercase
	
	if [[ -s ${DirName/bin/}etc/securecommands.cfg ]]
	then
		if find ${DirName/bin/}etc/securecommands.cfg -user root ! -perm -022 -print | read OK
		then
			if grep -q "^${ProgNameShort}:" ${DirName/bin/}etc/securecommands.cfg
			then
				grep -p "^${ProgNameShort}:" ${DirName/bin/}etc/securecommands.cfg \
				| grep -Ei "secureCommand|allowOpts|objectMaxFiles|objectType" \
				| sed 's/=/ = /' \
				| while read var eq value
				do
					case $var in 
					 securecommand)  secureCommand="${value}"
								;;
					 allowopts)		allowOpts="${value}"
								;;
					 objectmaxfiles) 	objectMaxFiles="${value}"
								;;
					 objecttype) 		objectType="${value}"
								;;
					 *)			:
								# log to syslog about mis-configuration!?
								;;
					esac
				done
			fi
		fi
	fi

	# config-commands start

	case $ProgNameShort in
	 cmd)		secureCommand='ops'
			allowOpts=''
			objectMaxFiles=32
			objectType="shellcmd"
			;;
	 ops)		secureCommand='ops'
			allowOpts=''
			objectMaxFiles=32
			objectType="shellcmd"
			;;
	 help)		secureCommand='help'
			allowOpts=''
			objectMaxFiles=0
			objectType="help"
			;;
	 mount)		: secureCommand="/usr/sbin/mount"
			allowedOpts='t:'
			objectMaxFiles=1
			;;
	 umount)	: secureCommand="/usr/sbin/umount"
			allowedOpts='ft:'
			objectMaxFiles=1
			;;
	 proccred)      secureCommand="/usr/bin/proccred"
			allowedOpts=''
			objectMaxFiles=1
			objectType="process"
			;;
	 procfiles)     secureCommand="/usr/bin/procfiles"
			allowedOpts=''
			objectMaxFiles=1
			objectType="process"
			;;
	 procflags)     secureCommand="/usr/bin/procflags"
			allowedOpts=''
			objectMaxFiles=1
			objectType="process"
			;;
	 procldd)       secureCommand="/usr/bin/procldd"
			allowedOpts=''
			objectMaxFiles=1
			objectType="process"
			;;
	 procmap)       secureCommand="/usr/bin/procmap"
			allowedOpts='FS'
			objectMaxFiles=1
			objectType="process"
			;;
	 procrun)       : secureCommand="/usr/bin/procrun"
			allowedOpts=''
			objectMaxFiles=1
			objectType="process"
			;;
	 procsig)       secureCommand="/usr/bin/procsig"
			allowedOpts=''
			objectMaxFiles=1
			objectType="process"
			;;
	 procstack)     secureCommand="/usr/bin/procstack"
			allowedOpts='F'
			objectMaxFiles=1
			objectType="process"
			;;
	 procstop)      : secureCommand="/usr/bin/procstop"
			allowedOpts=''
			objectMaxFiles=1
			objectType="process"
			;;
	 procwait)      : secureCommand="/usr/bin/procwait"
			allowedOpts='v'
			objectMaxFiles=1
			objectType="process"
			;;
	 procwdx)       secureCommand="/usr/bin/procwdx"
			allowedOpts='F'
			objectMaxFiles=1
			objectType="process"
			;;
	 rm)		secureCommand="/usr/bin/rm"
			allowedOpts='f'
			objectMaxFiles=1
			;;
	 rmdir)		secureCommand="/usr/bin/rmdir"
			allowedOpts=''
			objectMaxFiles=1
			;;
	 mkdir)		secureCommand="/usr/bin/mkdir"
			allowedOpts='em:'
			objectMaxFiles=1
			;;
	 truss)		secureCommand='/usr/bin/truss'
			allowedOpts='fcaldDeitx:s:m:r:w:u:o:p:'
			objectMaxFiles=1
			objectType="process"
			objectFlag='p'
			;;
	 chfs)		: secureCommand='/usr/sbin/chfs'
			allowedOpts='a:'
			objectMaxFiles=1
			objectType="filesystem"
			;;
	 strings)	secureCommand='/usr/bin/strings'
			allowedOpts='an:ot:'
			objectMaxFiles=32
			;;
	 ls)		secureCommand="/usr/bin/ls"
			allowedOpts='aAbcCdeEfFgHikLlmnNopqrRsStUuxX1'
			objectMaxFiles=32
			;;
	 od)		secureCommand="/usr/bin/od"
			allowedOpts=':A:j:N:tabcCdDefFhHiIlLOoPp:Sv:wxX'
			objectMaxFiles=1
			;;
	 dump)		secureCommand="/usr/bin/dump"
			allowedOpts='acdghlnopqrst:tuv:zHRT:X'
			objectMaxFiles=1
			;;
	 egrep)		secureCommand="/usr/bin/egrep"
			allowedOpts='bc:e:fhiln:pquwxy'
			objectMaxFiles=32
			;;
	 grep)		secureCommand='/usr/bin/egrep'
			allowedOpts='bcE:e:fhiln:pquwxy'
			objectMaxFiles=32
			;;
	 vi) 		secureCommand="/usr/bin/tvi"
			allowedOpts='Rc:lw:'
			objectMaxFiles=1
			objectType="write"
			# vi will use tvi, this will block anything untrusted as we could see
			# 
			;;
	 kill)		secureCommand="/usr/bin/kill"
			allowedOpts='1234567890s:'
			objectMaxfiles=1
			objectType="process"
			;;
	 wc)		secureCommand="/usr/bin/wc"
			allowedOpts='cklmw'
			objectMaxFiles=16
			;;
	 tail) 		secureCommand="/usr/bin/tail"
			allowedOpts="b:c:fk:m:n:r" 
			objectMaxFiles=1
			;;
	 head) 		secureCommand="/usr/bin/head"
			allowedOpts="c:n:" 
			objectMaxFiles=1
			;;
	 cat)		secureCommand="/usr/bin/cat"
			allowedOpts="benqrsStuvZ"
			objectMaxFiles=16
			;;
	 zcat)		secureCommand="/usr/bin/zcat"
			allowedOpts="n"
			objectMaxFiles=16
			;;
	 compress)	secureCommand="/usr/bin/compress"
			allowedOpts="b:cCfFnqv"
			objectMaxFiles=16
			;;
	 uncompress)	secureCommand="/usr/bin/uncompress"
			allowedOpts="cfFnq"
			objectMaxFiles=16
			;;
	 gzcat)		secureCommand="/usr/bin/gzip --decompress --stdout"
			objectMaxFiles=16
			allowedOpts=""
			;;
	 gzip)		secureCommand="/usr/bin/gzip"
			objectMaxFiles=16
			allowedOpts="cdfhlLnNqS:tvV109"
			;;
	 zcat)		secureCommand="/usr/bin/zcat"
			allowedOpts='nV'
			objectMaxFiles=16
			;;
	 touch)		secureCommand="/usr/bin/touch"
			allowedOpts='acfmr:t:'
			objectMaxFiles=1
			;;
	 cp)		secureCommand="/usr/bin/cp"
			allowedOpts='deE:fiI'
			objectMaxFiles=2
			sourceTarget=true
			objectType="fileord"
			;;
	 mv)		secureCommand="/usr/bin/mv"
			allowedOpts='deE:fiI'
			objectMaxFiles=2
			sourceTarget=true
			objectType="fileord"
			;;
	 split)		secureCommand="/usr/bin/split"
			allowedOpts='l:a:b:'
			objectMaxFiles=2
			sourceTarget="tertiary"		# target may not exist - and this is ok.
			objectType="fileord"
			;;
	 *) 		if [[ -z ${secureCommand} ]]
	 		then
	 	 		_errMsg "Unknown command or command not supported."
				exit 1
			fi
			;;
	esac
	# config-commands end
	
fi

# Process options
############################################################


# Who are you?
############################################################
TTYNUM=$( tty -s && tty )

if [[ -z $TTYNUM && $1 != @(--*config*) ]]
then
	_errMsg "You cannot run this via automation, it must be run interactively."
	exit 127
fi

TTYNUM=${TTYNUM##*/dev/}
TTYID=$( /usr/bin/ps -Xft${TTYNUM} -ouser= 2>/dev/null | grep -wv root | awk '$1 != "'$USER'" { print $1; exit }' )

if [[ ! -z $REALUSER && -z ${TTYID} ]]
then
	TTYID=${REALUSER}
fi

# User has changed to the same account!? 
# before it made sense to block this and ask them why,
# but with the new syntax it could actually make sense
# to have them run all the same commands as operations
# and this could block that!? So updating the message
# to make sure they are running as their own ID
# and not and application ID.
############################################################
if [[ ${SUDO_USER} == ${USER} ]]
then
	#_errMsg "Why do this to yourself?"
	_errMsg "You need to be logged in under your own account to run this command."
	exit 127
fi

if [[ -z ${TTYID} ]]
then
	_errMsg "Unable to identify user."
	exit 127
fi

# What groups are you a member of?
############################################################
_sourceGroups=( $( id -G -n $TTYID ) )

# And who are you really!?
############################################################

_longUserName=$( lsuser -a gecos ${TTYID} 2>/dev/null | cut -f2-99 -d"="  )

# @@SGM - Ideally need to update to situation defined getopts
# 	  this is a bit messy!
############################################################
# If the user asking for some help!?
############################################################
if [[ $1 == @(-h|-help|--help) && -z $2 ]]
then
	if /bin/tty -s
	then
		typeset    FOLD_COLS=$( tput cols )
		typeset    FOLD_OPTS="-w ${FOLD_COLS} -bs"
	else
		typeset    FOLD_OPTS="-w 80 -bs"
	fi

	# A user has called this in a SUDO context - we need to ignore this
	# and pass it on to the desired command - if the user wants
	# help, then he needs to ask for it by calling without SUDO.
	############################################################
	if [[ ! -z "${SUDO_COMMAND}${SUDO_USER}" ]]
	then
		:

	elif [[ $USER == "root" && -z "${SUDO_COMMAND}${SUDO_USER}" ]]
	then
		_documentation_1  | fold ${FOLD_OPTS}

		exit 0
	else
		_documentation_2 | fold ${FOLD_OPTS}
		_documentation_3 | fold ${FOLD_OPTS}

		exit 0
	fi

	# Fall through.
	############################################################
fi

# Check quickly the flags and a "-l" on its own will invoke our list function
# This will work both for thee user who needs it and for us
############################################################
if [[ $1 == "-ll"  && $2 == "-u" && ! -z $3 ]]
then
	: 
	# Place holder for listing users attributes in the
	# style of SUDO -ll -u bob
	############################################################
	_listOnly=1
	_fromUser=$3

elif [[ ${objectType} == "help" ]]
then
	_listOnly=99

elif [[ $1 == "-l" && -z $2 ]]
then
	_listOnly=1

elif [[ $1 == "-e" && -z $2 && ! -z "${SUDO_COMMAND}${SUDO_USER}" ]]
then	
	# Used is calling -e flag and they are not logged into this account.

	_errMsg "This is something you are not allowed to do."

	exit 1

elif [[ $1 == "-e" && -z $2 && -z "${SUDO_COMMAND}${SUDO_USER}" && ${USER} != "root" ]]
then
	# Disabled as non-root users for now as not sure how
	# to control the access outside of sudo

	_editOnly=1

elif [[ $1 == "-e" && -z "${SUDO_COMMAND}${SUDO_USER}" && ${USER} == "root" ]]
then
	# if we are trying to "edit" a file and the user 
	# is root then we are able to edit
	if [[ ! -z $2 ]]
	then
		if [[ $2 == "-" ]]
		then
			CONFIG=/usr/local/etc/${ProgName}.cfg

		elif id -u -n $2 >/dev/null 2>&1
		then
			CONFIG=/usr/local/etc/${2}.${ProgName}.cfg

		else
			_errMsg "The user '$2' does not exist."
			exit 1
		fi
	fi

	# Disabled as non-root users for now as not sure how
	# to control the access outside of sudo
	############################################################
	_editOnly=1

elif [[ $1 == "--unconfigure" && ${USER} == "root" ]]
then
	_unconfigureLinks;	exit $?

elif [[ $1 == "--configure" && ${USER} == "root" ]]
then
	_configureLinks; 	exit $?

elif [[ $1 == "--sudounconfig" && ${USER} == "root" ]]
then
	_sudoUnconfig;	exit $?

elif [[ $1 == "--sudoconfig" && ${USER} == "root" ]]
then
	_sudoConfig;	exit $?

elif [[ $1 == "--opsbinconfig" && ${USER} == "root" ]]
then

	_opsConfig;

	exit $?

elif [[ $1 == "--update-profile" ]]
then
	_opsConfigProfile $2;

	exit $?

else
	# No options are allowed here !?
	############################################################
	if [[ -z ${allowedOpts} ]]
	then
		if [[ $@ == @(-*) ]]
		then
			_errMsg "${ProgNameShort} does not allow these options."
			exit 1
		fi
	else
		# We check the options against the ones we have defined for this command
		############################################################
		while getopts ${allowedOpts} testOptions 2>&1
		do
			:
		done \
		| read optionResponse

		# If we have a response string then something is not correct
		############################################################
		if [[ ! -z ${optionResponse} ]]
		then
			# Sorry - have to do some cleaning up of messages
			# as ksh93 message returned is far to noisy when
			# used with -? and it could cause the user to think
			# that more options where available than actually are
			############################################################	
			optionResponse=${optionResponse/\[-MSG/\[-}
			optionResponse=${optionResponse/\[-a name\]/}
			optionResponse=${optionResponse/optstring name /}
			optionResponse=${optionResponse/*${ProgName}:/}

			_errMsg "${ProgNameShort}: ${optionResponse}"
			_errMsg "Invalid or insecure option for command: ${ProgNameShort}"

			exit 1
		else
			# We parse again to ensure we have the correct params at the end
			# of all the arguments, we also collect the objecname if
			# objectFlag matches ones of the arguments passed
			# this is so where we have the senario such as "truss -p pidid"
			# this becomes the objectname, where as its could also be "truss /bin/ls"
			############################################################

			fullArgs=${args[*]}
	
			while getopts ${allowedOpts} testOptions
			do
				if [[ ! -z ${objectFlag} ]]
				then
					if [[ ${testOptions} == ${objectFlag} && ! -z ${OPTARG} ]]
					then
						objectName=${OPTARG}
					fi
				fi
			done
	
			# Point to the end
			############################################################
			shift $(( $OPTIND-1 ))

			argsParsed="$*"

			if [[ ! -z $DEBUG ]]
			then
				print "[D] ARGV Remains: $*"
			fi
		fi
	fi

	# Are we alloed more than 1 filename for this command, ie cat command ??
	############################################################
	if (( $# > ${objectMaxFiles} ))
	then
		_errMsg "You are not allow to perform actions for ${ProgNameShort} on more than ${objectMaxFiles} file(s) at a time."
		exit 127
	fi

	# Check no one is playing silly buggers with the objectnames
	############################################################
	for objectNameCheck in ${objectName:-"$@"}
	do
		[[ ! -z $DEBUG ]] && print "[D] objectNameCheck=${objectNameCheck}"

		if [[ $objectType == "shellcmd" ]]
		then
			# No longer checking this here as we now will check further along
			# as these are great defaults for filesystem or process objects 
			# but not for operator commands where they are trying all sorts.
			############################################################
			#if [[  ${objectNameCheck} == @(*..*) || ${objectNameCheck} == @(*[\|\&\\\`\$;<>]*) ]]
			#then
			#	_errMsg "Invalid object specification. This has been logged as an attempted security violation." >&2
			#	_logger_err "Attempt to circumvent security of ${USER} by user ${TTYID} ${_longUserName} '$*'"
			#	exit 127
			#fi
			:
		elif [[ $objectType == "process"  ]]
		then
			if [[  ${objectNameCheck} == @(*..*) || ${objectNameCheck} == @(*[ %!\&\\\`\$\'\~<>]*) ]]
			then
				_errMsg "Invalid object specification. This has been logged as an attempted security violation." >&2
				_logger_err "Attempt to circumvent security of ${USER} by user ${TTYID} ${_longUserName} '$*'"
				exit 127
			fi
		else
			if [[ ${objectNameCheck} != @(/*) || ${objectNameCheck} == @(*..*) || ${objectNameCheck} == @(*[ %!\&\\\`\$\'\~<>]*) ]]
			then
				_errMsg "Invalid object specification. This has been logged as an attempted security violation." >&2
				_logger_err "Attempt to circumvent security of ${USER} by user ${TTYID}@${_longUserName} '$*'"
				exit 127
			fi
		fi
	done
fi
	
# We are editing the configuration file as the user who owns it.
############################################################
if (( $_editOnly > 0 ))
then
	if [[ ! -e ${CONFIG} ]]
	then
		_infoMsg "Creating ${CONFIG}"
		print "${header}" >${CONFIG}
		print "############################################################" >>${CONFIG}
		
		chmod 600 ${CONFIG}
	fi

	if [[ ${CONFIG} == @(${HOME}*) ]]
	then
		if find ${CONFIG} ! -user ${USER} -print | read OK
		then
			_errMsg "${CONFIG} must be owned by ${USER}"
			exit 1
		fi
		
		if ! find ${CONFIG} -perm 0600 -print | read OK
		then
			_errMsg "${CONFIG} must must only have read/write acess for ${USER}"
			exit 1
		fi
	else
		if [[ ! -w ${CONFIG} ]]
		then
			_errMsg "$USER You are not allowed to edit a global configuration, this configuration is locked by the system administrator."
			exit 1
		fi
	fi
	
	/usr/bin/vi '+$' ${CONFIG}

	# Ensure there are no invalid characters introduced into the configuration files
	############################################################
	if (( $( sed 's/[[:space:]]//g' ${CONFIG} | /usr/bin/tr -cd '[:cntrl:]' | /usr/bin/grep -cv '^$' ) > 0 ))
	then
		_errMsg "**WARNING** the file ${CONFIG} contains non-printable characters."
		_errMsg "**WARNING** the file ${CONFIG} contains non-printable characters."
		_errMsg "**WARNING** the file ${CONFIG} contains non-printable characters."
	fi

	if ! chown $USER ${CONFIG} 
	then
		_errMsg "Failed to update permissions, stopping."; exit 1
	fi

	if [[ $CONFIG == @(${HOME}*) ]]
	then
		if ! chmod 600 ${CONFIG}
		then
			_errMsg "Failed to update permissions, stopping."; exit 1
		fi

	elif [[ $USER == "root" ]]
	then
		if ! chmod 644 ${CONFIG}
		then
			_errMsg "Failed to update permissions, stopping."; exit 1
		fi
	fi
	
	# Simple check of the configuration
	############################################################
	while read var value 
	do
		[[ ${var} == @(#*) || -z ${var} || -z ${value} ]] && continue

		var=${var/\%/}		# ensure that SUDO type of % is removed we only support groups.

		if [[ "${var}${value}" == @(*[:graph:][:blank:][:cntrl:][:xdigit:]*) ]]
		then
			_errMsg "The group '${var}' is not valid, your configuration contains errors, which may include control characters."
		fi

		if ! grep -q "^${var}:" /etc/group
		then
			_errMsg "The group '${var}' does not exist, your configuration contains errors."
		fi
		
		if [[ -z "${value}" ]]
		then
			_errMsg "The group '${var}' does not have a valid object name, your configuraiton contains errors."
		fi

		if [[ ${var} == @(staff|wheel|users) ]]
		then
			_errMsg "The group ${var} is prevented from running securecommands by local security policy."
		fi

	done <${CONFIG}
	
	exit 0
fi

if (( $_listOnly > 98  ))
then
	:

elif [[ ! -s ${CONFIG} ]]
then
	_errMsg "${ProgName} is disabled or not configured."
	exit 127

elif [[ ${CONFIG} == @(${HOME}*) ]]
then
	# Lets check that the file has the correct permissions
	# as we dont tollerate insucure files.
	############################################################
	if ! /usr/bin/find ${CONFIG} -user ${USER} -perm 600 | read OK
	then
		_errMsg "The configuration file does not have the correct ownership or permissions"
		_errMsg "Please contact the account administrator to have the configuration corrected."
	
		exit 1
	fi
else
	# Lets check that the file has the correct permissions
	# as we dont tollerate insucure files. This configuration
	# is outside the home directory so it must be root owned
	# with no other write other than for root user.
	############################################################
	if ! /usr/bin/find ${CONFIG} -user root -perm 644 | read OK
	then
		_errMsg "The configuration file does not have the correct ownership or permissions"
		_errMsg "Please contact the account administrator to have the configuration corrected."
	
		exit 1
	fi
fi

# Load the configuration file so we can process more 
# easily when we have some wildcard based filenames in
# the configuration. The resultant array will contain the
# groups we are allowed allong with the expanded file
# names matched by the wild cards.
############################################################
# Note that this may not exist 
############################################################
/usr/bin/egrep -v "^#" ${CONFIG} 2>/dev/null \
| while read groupName fileSpec fileSpecParam
do
	[[ -z "${fileSpec}" ]] && continue
	
	# So - we can use this to verify users but we are not going
	# to document this as we do not want to encourage the use
	# of user based rules.
	############################################################
	if [[ ${groupName} == @(staff|wheel|%staff|%wheel|users|%users) ]]
	then
		continue	# we dont allow use of use of "global" user groups.

	elif [[ ${groupName} == @(\^*) ]]
	then
		[[ ${groupName/\^/} != ${SUDO_USER} ]] && continue
	else
		# If its not in the users group list then skip it.
		############################################################
		for _groupCheck in ${_sourceGroups[*]}
		do
			[[ ${_groupCheck} == ${groupName/\%/} ]] && break 
		done

		# If this does not match from above then skip to the next.
		[[ ${_groupCheck} != ${groupName/\%/} ]] && continue
	fi

	# Does is contain a wildcard? If so we only allow wildcards with a partial
	# filename, ie /* is not allowed, it needs to be more specific
	############################################################
	if [[ ${fileSpec} == @(*\**) && ${fileSpec} == @(*/\*) ]]
	then
		_errMsg "Specfication '${fileSpec}' in configuration is not allowed, please contact account owner".
		exit 1
	fi

	#
	############################################################
	if [[ $objectType == "process" ]]
	then
		# Get the Processes for the user we are running under
		# ie if via a sudo command this is the destination id.
		#
		# If running as root then we have visibility of all procs.
		############################################################	
		case $USER in
		 root)	/usr/bin/ps -eo pid,command,args;;
		 *) 	/usr/bin/ps -u $USER -o pid,command,args;;
		esac \
		| while read pid ppid command arguments
		do
			[[ ${ppid} == $$ ]] && continue
			[[ ${pid} == "PID" ]] && continue	# headers

			if [[ ${fileSpec} == @(*\**) && ${fileSpec} != @(*/\*) && ${command} == @(${fileSpec}) ]]
			then
				configData["${pid}-${command}"]="${groupName}"
				procsData["${pid}"]="${command}"

			elif [[ ${command} == ${fileSpec} ]]
			then
				configData["${pid}-${fileSpec}"]="${groupName}"
				procsData["${pid}"]="${fileSpec}"
			else
				: # configData["${fileSpec}"]="${groupName}"
			fi
		done

	elif [[ $objectType == "shellcmd" ]]
	then
		if [[ ${fileSpecParam} == @(-+--[[:space:]]*) ]]
		then
			shellCmdAllow["${fileSpec}"]="${fileSpecParam/-+--[[:space:]]/}"

		elif [[ ${fileSpecParam} == @(--+-[[:space:]]*) ]]
		then
			# This is where we will log stdout/stder
			############################################################
			shellCmdLogs["${fileSpec}"]="${fileSpecParam/--+-[[:space:]]/}"


		elif [[ ${fileSpecParam} == @(---+[[:space:]]*) ]]
		then
			# Help contextual information, skip
			############################################################
			shellCmdHelp["${fileSpec}"]="${fileSpecParam/---+[[:space:]]/}"
		else
			shellCmdData["${fileSpec}"]="${fileSpecParam}"
			configData["${fileSpec}"]="${groupName}"
		fi
	else	
		# So we have a wildcard that are happy with
		###########################################################
		if [[ "${fileSpec}" == @(*\**) && "${fileSpec}" != @(*/\*) ]]
		then
			# Expand the filenames and add to the index under the correct group
			############################################################
			set +o noglob
				for fileSpec in ${fileSpec}
				do
					configData[${fileSpec}]=${groupName}

					# In sourceTarget mode we store the target in 
					# a seperate array for checking later on
					############################################################
					if [[ ${sourceTarget} == "tertiary" ]]
					then
						sourceTarget=true
						configDataTarget["${fileSpec}"]=${configDataTarget["${fileSpec}"]}${fileSpecParam}" "
					elif [[ ${sourceTarget} == true && -e ${fileSpecParam} ]]
					then
						configDataTarget["${fileSpec}"]=${configDataTarget["${fileSpec}"]}${fileSpecParam}" "
					fi
				done
			set -o noglob
		else
			# No wildcard - fully specified filename.
			############################################################
			configData[${fileSpec}]=${groupName}

			# In sourceTarget mode we store the target in 
			# a seperate array for checking later on
			############################################################
			if [[ ${sourceTarget} == true && -e ${fileSpecParam} ]]
			then
				configDataTarget["${fileSpec}"]=${configDataTarget["${fileSpec}"]}${fileSpecParam}" "
			fi
		fi
	fi
done

[[ ! -z ${DEBUG} ]] && _debugVars shellCmdData shellCmdHelp shellCmdLogs configData configDataTarget procsData shellCmdAllow

if (( $_listOnly > 98 ))
then
	printf "%s\n############################################################\n" "Available Commands for User"

	set +o noglob

	ls -1 $HOME/etc/secure*.cfg $HOME/.secure* 2>/dev/null \
	| while read
	do
		for _groupCheck in ${_sourceGroups[*]}
		do
			if egrep -q "^[%]*${_groupCheck}[[:space:]]" ${REPLY} 2>/dev/null
			then
				REPLY=${REPLY##*/}
				REPLY=${REPLY%%.cfg}

				[[ $REPLY == @(.*) ]] && REPLY=${REPLY//.}

				echo "$REPLY"
			fi
		done
	done \
	| sort -u

	exit 0
	
elif (( $_listOnly > 0 ))
then
	printf "%s\n############################################################\n" "${header}"


	# Walk the objects and output them
	############################################################
	for objectItem in ${!configData[@]}
	do
		if [[ ! -z ${shellCmdData["${objectItem}"]} ]]
		then
			# if its and alias ! we want the help conttext data and
			# and not the filespec as we may not want to confuse people with this
			############################################################
			if [[ ! -z ${shellCmdHelp["${objectItem}"]} ]]
			then
				printf "%-15s %s %s\n" "${configData[${objectItem}]}" "${objectItem}" "${shellCmdHelp["${objectItem}"]}"

			elif [[ ${shellCmdData["${objectItem}"]} == @(----[[:space:]]*) ]]
			then
				# Anything goes ?
				############################################################
				if [[ ${shellCmdData["${objectItem}"]} == @(*\*) ]]
				then
					numberOfFields=-1
				else
					print -r -- ${shellCmdData["${objectItem}"]} \
					| awk -F'$' \
					'BEGIN { 
						count=0;
						}
					{
						for (i=2; i<=NF; i++)
						{
							if ($i ~ /^[1-9]/ && ! seen[$i+0]++ ) { count++ }
						}
					} END {
						print count;
						} ' \
					| read numberOfFields
				fi

				if (( ${numberOfFields} < 0 ))
				then	
					printf "%-15s %s 	%s\n" "${configData[${objectItem}]}" "${objectItem}" "Command accepts any parameters."

				elif (( ${numberOfFields} == 0 ))
				then
					printf "%-15s %s 	%s\n" "${configData[${objectItem}]}" "${objectItem}" "Command accepts no parameters."

				elif (( ${numberOfFields} > 1 || ${numberOfFields} == 0 ))
				then
					#printf "%-15s %s 	%s\n" "${configData[${objectItem}]}" "${objectItem}" "Command accepts ${numberOfFields} parameters."
					printf "%-15s %s 	%s\n" "${configData[${objectItem}]}" "${objectItem}" "${shellCmdData["${objectItem}"]}"
				else
					#printf "%-15s %s 	%s\n" "${configData[${objectItem}]}" "${objectItem}" "Command accepts ${numberOfFields} parameter."
					printf "%-15s %s 	%s\n" "${configData[${objectItem}]}" "${objectItem}" "${shellCmdData["${objectItem}"]}"
				fi
			else
				printf "%-15s %s %s\n" "${configData[${objectItem}]}" "${objectItem}" "${shellCmdData["${objectItem}"]}"
			fi
		else
			[[ ! -z ${DEBUG} ]] && _debugVars objectItem

			# If we are in "source" "target" mode we show the target param
			# along side the source.
			############################################################
			if [[ ! -z ${configDataTarget["${objectItem}"]} ]]
			then
				for filespec in ${configDataTarget["${objectItem}"]}
				do
					printf "%-15s %s %s\n" "${configData[${objectItem}]}" "${objectItem}" "${filespec}"
				done
			else
				printf "%-15s %s\n" "${configData[${objectItem}]}" "${objectItem}"
			fi
		fi
	done \
	| sort -u +1	# sort the command alphabetlically for quick user lookup.

	# Early bath
	############################################################
	exit 0
fi

# Empty object list ?
############################################################
if [[ -z "$@" && -z ${objectName} ]]
then
	_errMsg "No object name supplied."
	exit 127
fi

# Check the groups allowed to action this object.
# If objectName is null then the objects will be in $@
# if its not-null then its allready passed using flags
# ie "truss -p pid".
############################################################
if [[ -z $objectName ]]
then
	if [[ $objectType == "shellcmd" ]]
	then
		[[ ! -z ${DEBUG} ]] && echo "[D] \$*=$*"

		objectName=$1; shift

		if [[ -z ${configData["${objectName}"]} ]]
		then
			let ERC++
		else
			# OK - This is something special - its an alias for a command
			# allowing much more comlex commands to be run like an alias
			# so...
			# admins stopall ---- /usr/local/bin/stop_all.sh -h now -u $1 -w $2
			############################################################	
			if [[ ${shellCmdData["${objectName}"]} == @(----[[:space:]]*) ]]
			then
				# Re-write the command.
				############################################################
				let cmdAlias++
				shellCmdData["${objectName}"]=${shellCmdData["${objectName}"]/----[[:space:]]/}

				if [[ ${shellCmdData["${objectName}"]} == @(*\*) ]]
				then
					numberOfFields=-1
				else
					# Find the number of fields required for this command
					# ie. $1 $2 $3 etc...
					# as a cheat you can do ${1} and not be counted !
					############################################################
					print -r -- ${shellCmdData["${objectName}"]} \
					| awk -F'$' \
					'BEGIN {
						count=0;
						}
					{
						for (i=2; i<=NF; i++)
						{
							if ($i ~ /^[1-9]/ && ! seen[$i+0]++ ) { count++ }
						}
					} END {
						print count;
						} ' \
					| read numberOfFields
				fi
		
				if (( numberOfFields > 0 ))
				then
					if [[ ${shellCmdData["${objectName}"]} == @(*\$\{*) ]]
					then
						if (( $# < ${numberOfFields} ))
						then
							_errMsg "The number of required fields is ${numberOfFields}, and you supplied $#."
							_errMsg "usage: ${objectName} ${shellCmdHelp["${objectName}"]}"
							exit 1
						fi
					else

						if (( $# < ${numberOfFields} || $# > ${numberOfFields} ))
						then
							_errMsg "The number of required fields is ${numberOfFields}, and you supplied $#."
							_errMsg "usage: ${objectName} ${shellCmdHelp["${objectName}"]}"
							exit 1
						fi
					fi
				fi

				if (( $# > 0 && numberOfFields == 0 ))
				then
					_errMsg "This command does not accept any parameters."
					exit 1
				fi

				# @@SGM - special parsing... does the help text contain a regexp of data that
				#			     we must adhere to ?
				###########################################################	
				if [[ ${shellCmdHelp["${objectName}"]} == @(*\[*) ]]
				then
					print -r -- ${shellCmdHelp["${objectName}"]} \
					| awk '
					{	
						# Field seperation using [ bobobob ]
						############################################################
						RS="[{}]"; 

						# Loop through the fields
						############################################################
						for (i=1; i<=NF; i++) 
						{ 	
							# Does it start with [ and end with ]
							############################################################
							if ($(i) ~ /^\[/ && $(i) ~ /\]$/) 
							{ 	
								printf "%d %s\n",++fields,substr($(i),2,length($(i))-2); 
							} 	
						} 
					}' \
					| while read var value
					do
						if [[ ! -z ${DEBUG} ]]
						then
							echo "[D] ${var}='${value}'"
						fi

						# ${args} contains the $0 at the start.
						# we skip args[0] as this is the appname
						############################################################
						if [[ ${args[${var}]} != @(${value}) ]]
						then
							_errMsg "Argument passed is not within allowed range."
							_errMsg "usage: ${objectName} ${shellCmdHelp["${objectName}"]}"
							exit 1
						fi
					done
				fi

			elif [[ -z ${shellCmdData["${objectName}"]} ]]
			then
				# So there are no options specified, therefore you shouldnt have any!?
				############################################################
				[[ ! -z $1 ]] && let ERC++
			else
				# Wildcards, we need to ensure that its clear to the user
				# which if the wildcards that we are going to be using
				# either regexp/egrep/shell, unless we are going to configure 
				# somehting that allows a selection of these !?
				############################################################	
				if [[ ${shellCmdData["${objectName}"]} == @(*\**) ]]
				then
					# We have wildards on the the objects parameters, 
					# lets see if we can match this up.
					############################################################	
					if [[ ${configData["${objectName}"]} == @(*\**) ]]
					then						
						if [[ "${saveArgs}" != @(${configData["${objectName}"]}) ]]
						then
							# we can validate against egrep !?
							if echo "${saveArgs}" | egrep -q "${configData["${objectName}"]}"
							then
								:
							else
								let ERC++
							fi
						fi
					fi
					
					if [[ $@ != @(${shellCmdData["${objectName}"]}) ]]
					then
						let ERC++
					fi
				else
					if [[ "$@" != "${shellCmdData["${objectName}"]}" ]]
					then
						let ERC++
					fi
				fi
			fi
		fi
	else
		# is this a PID and does it exist in our procsData?
		############################################################
		if [[ $objectType == "process" ]]
		then
			for objectName in $@
			do
				# So we have a pid, lets lookup the app name and compare to
				# config data to ensure we have a full house
				############################################################
				if [[ -n ${procsData[${objectName}]} ]]
				then
					let ERC=0; break
				else
					let ERC++
				fi
			done

		elif [[ $objectType == "fileord" ]]
		then
			ERC=1

			[[ ! -z ${DEBUG} ]] && _debugVars args

			pointer=0

			lastObjectName=${args[$(( ${#args[*]}-1 ))]}	# our target file

			for objectName in $@
			do
				let pointer++

				if (( ${pointer} < $# ))
				then	
					if [[ -z ${configDataTarget["${objectName}"]} ]]
					then
						let ERC++
					else
						for filespec in ${configDataTarget["${objectName}"]}
						do
							if [[ "${lastObjectName}" == ${filespec} ]]
							then
								ERC=0
								continue
							fi
						done
					fi
				fi
			done
		else

			for objectName in $@
			do
				if [[ ! -z ${configData[${objectName}]} ]]
				then
					let ERC=0; #break
				else
					let ERC++
				fi
			done
		fi

	fi
else
	# Object Name has been passed using some flags
	############################################################ 
	if [[ ! -z ${configData[${objectName}]} ]]
	then
		let ERC=0; break
	else
		let ERC++
	fi

	# is this a PID and does it exist in our procsData?
	############################################################
	if [[ $objectType == "process" && ! -z ${procsData[${objectName}]} ]]
	then
		# So we have a pid, lets lookup the app name and compare to
		# config data to ensure we have a full house
		############################################################
		if [[ ! -z ${configData[${procsData[${objectName}]}]} ]]
		then
			let ERC=0; break
		else
			let ERC++
		fi
	fi
fi

# Early bath
############################################################
if (( $ERC >0 ))
then
	if [[ ${sourceTarget} == true ]]
	then
		objectName="${argss}"
	fi

	if [[ -n ${argsParsed} ]]
	then
		_errMsg "You do not have the required permission to access ${argsParsed}"
	else
		_errMsg "You do not have the required permission to access ${objectName}"
	fi

	_logger_err "File ${objectName} user ${TTYID}@${TTYNUM} '${_longUserName}' has no permission to access this object. Command='${saveArgs}' USER=${USER}"

	exit 127
fi

# Lets get serious as we are about to execute the required
# commands and we want to ensure that we capture the exit
# of the commands if they fail or are interupted
############################################################
if [[ ! -z ${DEBUG} ]]
then
	:
else
	#trap '_trapcntrlc $?;' 1 2 3 11 15 EXIT
	trap '_trapcntrlc $?;' 1 2 11 15 EXIT
fi

# Commands that are read only we can safely run from one function rather than splitting them out
# as we do not need to do any clever handling of the the files or return codes/error capture.
############################################################
if [[ $objectType == @(read|fileord) ]]
then
	_logger "running: $secureCommand ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER}"

	$secureCommand ${saveArgs} 

	ERC=$?

	if (( ERC > 0 ))
	then
		_logger "running: $secureCommand ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER} complete with failures, return=${ERC}"
	else
		_logger "running: $secureCommand ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER} complete, return=${ERC}"
	fi

elif [[ $objectType == "process" ]]
then
	_logger "running: $secureCommand ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER}"

	$secureCommand ${saveArgs} 

	ERC=$?

	if (( ERC > 0 ))
	then
		_logger "running: $secureCommand ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER} complete with failures, return=${ERC}"
	else
		_logger "running: $secureCommand ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER} complete, return=${ERC}"
	fi
else
	if [[ $ProgNameShort == "ops" || $ProgNameShort == "cmd" ]]
	then
		# The operators have landed...
		#  Source the appropriate commands
		############################################################
		typeset executionShell=$( lsuser -a shell $USER 2>/dev/null | cut -f2 -d"=" )
		
		if [[ -z ${executionShell} ]]
		then
			[[ -e ${HOME}/.cshrc && -e /usr/bin/csh ]]   && executionShell="/usr/bin/csh"
			[[ -e ${HOME}/.basrc && -e /usr/bin/bash ]]  && executionShell="/usr/bin/bash"
			[[ -e ${HOME}/.basrc && -e /usr/bin/bash ]]  && executionShell="/usr/bin/bash"
			[[ -e ${HOME}/.profile && -e /usr/bin/ksh ]] && executionShell="/usr/bin/ksh"
		fi

		case ${executionShell} in
		 /*csh)	
			shellInit="source $HOME/.cshrc"
			;;
		 */*ksh*|*/psh|*/tsh)	
		 	shellInit=". $HOME/.profile"
			;;
		 */bsh)	
			shellInit=". $HOME/.bashrc"
			;;
		 *)
			shellInit=""
			_errMsg "We do not recognise the shell $executionShell, this may fail."
			;;
		esac

		if [[ ! -s ${shellInit//. } ]]
		then
			shellInit="true"
		fi
		
		if (( ${cmdAlias} > 0 ))
		then
			if [[ ! -z ${shellCmdLogs[${objectName}]} ]]
			then
				# More than one parameter
				############################################################
				if [[ ${shellCmdLogs[${objectName}]} == @(*[[:space:]]*) ]]
				then
					shellCmdLogsPerms=${shellCmdLogs[${objectName}]}
					shellCmdLogsPerms=${shellCmdLogsPerms##* }

					shellCmdLogs["${objectName}"]=${shellCmdLogs[${objectName}]%% *}
				fi
		
				# This could be a wildcard or $HOME
				############################################################
				logOutput=$( set +o noglob; eval echo "${shellCmdLogs[${objectName}]}" )
				
				# Remove any trailing / to keep it clean
				############################################################
				logOutput=${logOutput/%\//}

				if [[ ! -e ${logOutput} ]]
				then
					_errMsg "The desired log location does not exist, cannot continue without logging."
					exit 1
				fi

				if [[ ! -w ${logOutput} ]]
				then
					_errMsg "The desired log location is not writable, cannot continue without logging."
					exit 1
				fi
				
				epoc=$( /usr/bin/date +%s )

				logOutput="${logOutput}/${TTYID}_${TTYNUM//\/}.${epoc}.log"
			fi

			# Restore the arguments and shift by one to remove the alias
			# we need to be convinced there is nothing in this data that 
			# could cause accidental escalation - nothing should
			# exists in the data that could be interperated as a shell command!
			############################################################	
			set -- ${saveArgs}; shift

			
			# Double double validation!?
			############################################################
			if [[ $@ == @(*[\$\`\;\&\|\>\<\[\][:cntrl:]]*) ]]
			then
				# You could have only slipped this far if you are a shell command !
				# and now we are sending you home if you there's no overrides set 
				############################################################	
				if [[ ! -z ${shellCmdAllow[${objectName}]} ]]
				then
					if [[ ${@//[${shellCmdAllow[${objectName}]}]} == @(*[\$\`\;\&\|\>\<\[\][:cntrl:]]*) ]]
					then
						_errMsg "Invalid Command."
						_logger_err "Attempt to circumvent security of ${USER} by user ${TTYID} ${_longUserName} '$*'"
						exit 1
					fi
				else
					_errMsg "Invalid Command."
					_logger_err "Attempt to circumvent security of ${USER} by user ${TTYID} ${_longUserName} '$*'"
					exit 1
				fi
			fi

			# Restore the GLOB for this to ensure we log the full command.
			# using echo now as not 100% about using print --
			############################################################
			if [[ ${shellCmdData["${objectName}"]} != @(*\*) ]]
			then
				executeCommand=$( set +o noglob; eval echo ${shellCmdData[${objectName}]}  )
			else
				# We remove the wildard and append the params passed at runtime
				############################################################
				executeCommand=$( eval echo ${shellCmdData[${objectName}]//\*/} $* )
			fi

			if [[ $? != 0 ]]
			then
				_errMsg "Invalid Command."
				exit 1
			fi

			# @@SGM should we test that no errors are returned from profile execution!?
			############################################################
			_logger "running: ${executeCommand} from ${TTYID}@${TTYNUM} as ${USER}"

			# Restore /etc/environnment
			# We have pre-checked permissions on this at the start so its safe.
			############################################################
			eval $( awk '!/^#/ && $1 ' /etc/environment ) >/dev/null 2>&1

			# Execute the command and throw away for now stderr/stdout from shellInit
			############################################################
			if [[ ! -z ${logOutput} ]]
			then
				>${logOutput}

				if ! chown ${USER} "${logOutput}"
				then
					_errMsg "${logOutput} changed by another user, stopping."
					exit 1
				fi
					
				if ! chmod ${shellCmdLogsPerms} "${logOutput}"
				then
					_errMsg "${logOutput} changed by another user, stopping."
					exit 1
				fi

				echo "# ${executeCommand} from ${TTYID}@${TTYNUM} as ${USER} $( date )\n############################################################" >>${logOutput}

				(
					# CSH likes single comands from STDIN not the cmd line
					############################################################
					if [[ ${executionShell} == @(*csh) ]]
					then
						echo "${shellInit} >&/dev/null; ${executeCommand}" \
						| ${executionShell} -t
					else
						# Oversized argument...
						if (( ${#executeCommand} > 255-( ${#shellInit}+16 ) )) 
						then
							echo "${shellInit} >/dev/null 2>&1; ${executeCommand}" \
							| ${executionShell} -t -s
						else
							${executionShell} -t "${shellInit} >/dev/null 2>&1; ${executeCommand}" 
						fi
					fi
					_saveERC $?
				) 2>&1 \
				| tee -a ${logOutput}

				_restoreERC; 
				ERC=$?

				echo "###########################################################\n# $( date )" >>${logOutput}

				if (( ERC > 0 ))
				then
					echo "# complete with failures, return=${ERC}" >>${logOutput}
				else
					echo "# complete, return=${ERC}" >>${logOutput}
				fi
			else
				# CSH likes single comands from STDIN not the cmd line
				############################################################
				if [[ ${executionShell} == @(*csh) ]]
				then
					echo "${shellInit} >&/dev/null; ${executeCommand}" \
					| ${executionShell} -t
				else
					# Oversized argument... anything over 255 is a problem
					# for some unknown reason - will check with ksh source
					# code, until then we need to workaround.
					############################################################
					if (( ${#executeCommand} > 255-( ${#shellInit}+16 ) )) 
					then
						echo "${shellInit} >/dev/null 2>&1; ${executeCommand}" \
						| ${executionShell} -t -s
					else
						${executionShell} -t "${shellInit} >/dev/null 2>&1; ${executeCommand}"
					fi
				fi
				ERC=$?
			fi

		else
			if [[ $@ == @(*[\$\`\;\&\|\>\<\[\][:cntrl:]]*) ]]
			then
				_errMsg "Invalid Command."
				_logger_err "Attempt to circumvent security of ${USER} by user ${TTYID} ${_longUserName} '$*'"
				exit 1
			fi

			# Pass execution over to the shell, making sure we source whatever profile is required
			# and use the saved arguments as these have not been tainted.
			# throw away the shellInit output
			############################################################
			_logger "running: ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER}"

			if [[ ! -z ${logOutput} ]]
			then
				>${logOutput}

				if ! chown ${USER} "${logOutput}"
				then
					_errMsg "${logOutput} changed by another user, stopping."
					exit 1
				fi
					
				if ! chmod ${shellCmdLogsPerms} "${logOutput}"
				then
					_errMsg "${logOutput} changed by another user, stopping."
					exit 1
				fi

				echo "# ${executeCommand} from ${TTYID}@${TTYNUM} as ${USER} $( date )\n############################################################" >>${logOutput}

				(
					# CSH likes single comands from STDIN not the cmd line
					############################################################
					if [[ ${executionShell} == @(*csh) ]]
					then
						echo "${shellInit} >&/dev/null; ${saveArgs}" \
						| ${executionShell} -t
					else
						${executionShell} -t "${shellInit} >/dev/null 2>&1; ${saveArgs}"
					fi
					_saveERC $?
				) 2>&1 \
				| tee -a ${logOutput} 

				_restoreERC; 
				ERC=$?

				echo "###########################################################\n# $( date )" >>${logOutput}

				if (( ERC > 0 ))
				then
					echo "# complete with failures, return=${ERC}" >>${logOutput}
				else
					echo "# complete, return=${ERC}" >>${logOutput}
				fi
			else
				# CSH likes single comands from STDIN not the cmd line
				############################################################
				if [[ ${executionShell} == @(*csh) ]]
				then
					echo "${shellInit} >&/dev/null; ${saveArgs}" \
					| ${executionShell} -t
				else
					${executionShell} -t "${shellInit} >/dev/null 2>&1; ${saveArgs}"
				fi
				ERC=$?
			fi
		fi

		if (( ERC > 0 ))
		then
			_logger "running: ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER} complete with failures, return=${ERC}"
		else
			_logger "running: ${saveArgs} from ${TTYID}@${TTYNUM} as ${USER} complete, return=${ERC}"
		fi
		
	
	elif [[ $ProgNameShort == "vi" ]]
	then
		# If the file is bigger than a few MB then we should not be vi'ing it
		############################################################
		if /usr/bin/find ${objectName} -size +2097152c -print 2>/dev/null | read isBigger
		then
			_errMsg "File is to large for normal editing."
			exit 127
		fi
	
		if [[ -e ${objectName}.securevi ]]
		then
			# Check if there is really a session in play here
			############################################################
			if ps -u ${USER} -oTHREAD | grep -q ${objectName}.securevi
			then
				_errMsg "The file is already locked by someone else, you cannot edit this file."
				exit 127
			fi
		fi
	 
		if [[ -e $HOME/.securevi.log ]]
		then
			_secureVI_log=$HOME/.securevi.log  	
		elif [[ -d $HOME/log ]]
		then
		  	_secureVI_log=$HOME/log/securevi.log
		else
		  	_secureVI_log=$HOME/.securevi.log
		fi
	 
		if [[ -e ${_secureVI_log} ]]
		then
			if ! find ${_secureVI_log} -user ${USER} -print | read OK
			then
				_errMsg "${_secureVI_log} has incorect ownership, I cannot continue."
				exit 1
			fi
			
			if ! find ${_secureVI_log} -perm 0600 -print | read OK
			then
				_errMsg "${HOME}/.securevi.log must must only have read/write acess for ${USER}, I cannot continue."
				exit 1
			fi
		else
			>${_secureVI_log}
			chmod 600 ${_secureVI_log}
		fi
		
		# 
		############################################################
		if ! /usr/bin/cp -p ${objectName} ${objectName}.securevi
		then
			_errMsg "Unable to save a copy of the file, there is not enough storage available."
		
			[[ -e ${objectName}.securevi ]] && rm ${objectName}.securevi
		
			exit 127
		fi
	
		_logger "File ${objectName} locked by ${TTYID}@${TTYNUM} for updates"

		# Is this a Vormetric Guard Point!?
		/bin/df ${objectName} 2>/dev/null \
		| awk 'NR>1 { print $NF }' \
		| read objectFilesystem
    
		/usr/sbin/mount 2>/dev/null \
		| awk '$3 == "secfs" { print $1 }' \
		| grep -q "^${objecFilesystem}$" \
		| read objecFilesystemEncrypted
    
		if [[ ! -z ${objecFilesystemEncrypted} ]]
		then
			EXINIT="set hist=1024 shell=/bin/false noexrc bf dir=${objectName##*/} novice"
		else
			EXINIT="set novice hist=1024 shell=/bin/false noexrc bf "
		fi
  	
		export EXINIT
    
		PATH='' $secureCommand ${objectName}.securevi
	
		if /usr/bin/cmp -s ${objectName}.securevi ${objectName} 2>/dev/null
		then
			print "[I] No changes made."
			
			_logger "File ${objectName} locked by ${TTYID}@${TTYNUM} no changes detected"
		else
			/usr/bin/diff -u ${objectName}.securevi ${objectName} >>${_secureVI_log} 2>&1
		
			if ! /usr/bin/cp ${objectName}.securevi ${objectName}
			then
				_errMsg "Failed to update ${objectName} possible storage issue."
		
				/usr/bin/rm ${objectName}.securevi
			fi
	
			print "[I] ${objectName} updated."
	
			_logger "File ${objectName} locked by ${TTYID}@${TTYNUM}, updated $HOME/.securevi.log"
		fi

		# Clean up
		############################################################
		rm -f ${objectName}.securevi
	else
		_errMsg "Sorry, but I really dont know what to do with ${ProgNameShort} or ${objectName}"
		exit 127
	fi
fi

# Termination
############################################################
exit ${ERC}
